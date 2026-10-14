package com.harness.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harness.admin.config.FileStorageService;
import com.harness.admin.domain.digitalemployee.ReleaseStatus;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseResponse;
import com.harness.admin.domain.knowledgebase.KnowledgeBaseStatus;
import com.harness.admin.mapper.DigitalEmployeeReleaseMapper;
import com.harness.admin.mapper.KnowledgeBaseMapper;
import com.harness.admin.mapper.entity.DigitalEmployeeRelease;
import com.harness.admin.mapper.entity.KnowledgeBase;
import com.harness.admin.service.KnowledgeBaseService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件说明：数字员工发布与回滚接口集成测试。
 *
 * <p>覆盖发布成功（版本生成与状态更新）、再次发布版本递增、
 * 关联资源未就绪发布被拒（失败原因保留）、回滚生成新生效记录（历史快照不变）、
 * 普通用户无法发布。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@SpringBootTest
@AutoConfigureMockMvc
class DigitalEmployeeReleaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private DigitalEmployeeReleaseMapper releaseMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * 验证发布成功并生成版本，再次发布版本递增。
     */
    @Test
    void publish_shouldSucceedAndIncrementVersion() throws Exception {
        long employeeId = createEmployeeWithReadySkill("发布知识库", "doc.txt", "发布内容", "发布助手");
        String token = loginToken("admin", "Admin@12345");

        MvcResult first = publish(employeeId, token);
        assertThat(JsonPath.<String>read(first.getResponse().getContentAsString(), "$.data.status"))
                .isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(first.getResponse().getContentAsString(), "$.data.version")).isEqualTo("1");

        MvcResult second = publish(employeeId, token);
        assertThat(JsonPath.<String>read(second.getResponse().getContentAsString(), "$.data.version")).isEqualTo("2");

        // 数字员工发布状态与版本更新
        MvcResult detail = detail(employeeId, token);
        assertThat(JsonPath.<String>read(detail.getResponse().getContentAsString(), "$.data.status"))
                .isEqualTo("PUBLISHED");
        assertThat(JsonPath.<String>read(detail.getResponse().getContentAsString(), "$.data.publishedVersion"))
                .isEqualTo("2");
    }

    /**
     * 验证发布时知识库不可用被拒绝，且保留失败原因记录。
     */
    @Test
    void publish_withNotReadyKnowledgeBase_shouldFailWithReason() throws Exception {
        // 先构建可用链路（READY 知识库 -> Skill -> 数字员工）
        long kbId = uploadAndAwait("doc.txt", "text/plain", "发布内容", "可用知识库", KnowledgeBaseStatus.READY);
        long skillId = createSkillWithKb("可用技能", kbId);
        long employeeId = createEmployeeWithSkill("发布助手", skillId);
        // 破坏知识库：删除原始文件并重处理，最终进入 FAILED
        KnowledgeBase kb = knowledgeBaseMapper.selectById(kbId);
        fileStorageService.delete(kb.getStorageKey());
        knowledgeBaseService.reprocess(kbId);
        KnowledgeBaseResponse broken = knowledgeBaseService.detail(kbId);
        for (int i = 0; i < 60 && broken.status() != KnowledgeBaseStatus.FAILED; i++) {
            Thread.sleep(200);
            broken = knowledgeBaseService.detail(kbId);
        }

        String token = loginToken("admin", "Admin@12345");
        MvcResult result = publish(employeeId, token);

        String content = result.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(content, "$.code")).isEqualTo("DIGITAL_EMPLOYEE_NOT_PUBLISHABLE");
        // 失败原因保留在发布记录中
        Long failedCount = releaseMapper.selectCount(new LambdaQueryWrapper<DigitalEmployeeRelease>()
                .eq(DigitalEmployeeRelease::getDigitalEmployeeId, employeeId)
                .eq(DigitalEmployeeRelease::getStatus, ReleaseStatus.FAILED));
        assertThat(failedCount).isGreaterThanOrEqualTo(1);
    }

    /**
     * 验证回滚生成新生效记录且历史快照不变。
     */
    @Test
    void rollback_shouldCreateNewRecordWithoutChangingHistoricalSnapshot() throws Exception {
        long employeeId = createEmployeeWithReadySkill("回滚知识库", "doc.txt", "回滚内容", "回滚助手");
        String token = loginToken("admin", "Admin@12345");
        MvcResult first = publish(employeeId, token);
        long release1Id = ((Number) JsonPath.read(first.getResponse().getContentAsString(), "$.data.id")).longValue();
        publish(employeeId, token);

        // 记录历史快照内容（不可变校验基准）
        DigitalEmployeeRelease historical = releaseMapper.selectById(release1Id);
        String snapshotBefore = historical.getConfigSnapshot();

        MvcResult rollback = mockMvc.perform(post("/api/digital-employees/" + employeeId + "/rollback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"releaseId\":" + release1Id + "}")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        String rollbackContent = rollback.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(rollbackContent, "$.code")).isEqualTo("SUCCESS");
        assertThat(JsonPath.<String>read(rollbackContent, "$.data.version")).isEqualTo("3");

        // 历史快照未被修改
        DigitalEmployeeRelease after = releaseMapper.selectById(release1Id);
        assertThat(after.getConfigSnapshot()).isEqualTo(snapshotBefore);

        // 当前生效版本切换为新记录
        MvcResult detail = detail(employeeId, token);
        assertThat(JsonPath.<String>read(detail.getResponse().getContentAsString(), "$.data.publishedVersion"))
                .isEqualTo("3");
    }

    /**
     * 验证普通用户无法发布。
     */
    @Test
    void publish_withoutAdmin_shouldReject() throws Exception {
        registerUser("releaseuser", "password123");
        String userToken = loginToken("releaseuser", "password123");

        MvcResult result = mockMvc.perform(post("/api/digital-employees/1/publish")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(JsonPath.<String>read(result.getResponse().getContentAsString(), "$.code"))
                .isEqualTo("RESOURCE_FORBIDDEN");
    }

    /**
     * 发布并返回结果。
     */
    private MvcResult publish(long employeeId, String token) throws Exception {
        return mockMvc.perform(post("/api/digital-employees/" + employeeId + "/publish")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * 查询数字员工详情。
     */
    private MvcResult detail(long employeeId, String token) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/digital-employees/" + employeeId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
    }

    /**
     * 创建关联 READY 知识库 Skill 的数字员工。
     */
    private long createEmployeeWithReadySkill(String kbName, String fileName, String content, String employeeName)
            throws Exception {
        long kbId = uploadAndAwait(fileName, "text/plain", content, kbName, KnowledgeBaseStatus.READY);
        long skillId = createSkillWithKb(kbName + "技能", kbId);
        return createEmployeeWithSkill(employeeName, skillId);
    }

    /**
     * 创建数字员工（单个 Skill）。
     */
    private long createEmployeeWithSkill(String name, long skillId) throws Exception {
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"" + name + "\",\"description\":\"描述\",\"systemPrompt\":\"系统提示词\","
                + "\"skillIds\":[" + skillId + "],\"modelConfig\":{\"model\":\"configured-model\"}}";
        MvcResult result = mockMvc.perform(post("/api/digital-employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    /**
     * 创建关联指定知识库的 Skill。
     */
    private long createSkillWithKb(String name, long kbId) throws Exception {
        String token = loginToken("admin", "Admin@12345");
        String body = "{\"name\":\"" + name + "\",\"description\":\"\",\"content\":\"能力配置\","
                + "\"knowledgeBaseIds\":[" + kbId + "]}";
        MvcResult result = mockMvc.perform(post("/api/skills")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    /**
     * 上传知识库并等待指定终态。
     */
    private long uploadAndAwait(String fileName, String contentType, String content, String name,
                                KnowledgeBaseStatus expected) throws Exception {
        String token = loginToken("admin", "Admin@12345");
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType,
                content.getBytes(StandardCharsets.UTF_8));
        MvcResult result = mockMvc.perform(multipart("/api/knowledge-bases")
                        .file(file)
                        .param("name", name)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long kbId = ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
        KnowledgeBaseResponse current = knowledgeBaseService.detail(kbId);
        for (int i = 0; i < 60 && current.status() != expected; i++) {
            Thread.sleep(200);
            current = knowledgeBaseService.detail(kbId);
        }
        return kbId;
    }

    /**
     * 注册测试用户。
     */
    private void registerUser(String username, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk());
    }

    /**
     * 登录并提取凭证。
     */
    private String loginToken(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.<String>read(result.getResponse().getContentAsString(), "$.data.token");
    }
}
