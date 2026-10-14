package com.harness.admin.controller;

import com.harness.admin.domain.common.ApiResponse;
import com.harness.admin.domain.common.PageResult;
import com.harness.admin.domain.previewchat.ChatRequest;
import com.harness.admin.domain.previewchat.ChatResponse;
import com.harness.admin.domain.previewchat.ConversationResponse;
import com.harness.admin.domain.previewchat.MessageResponse;
import com.harness.admin.service.PreviewChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文件说明：数字员工对话接口。
 *
 * <p>提供数字员工页的对话发起/延续、答复与历史查询能力，对已登录用户开放；
 * 普通用户仅可与已发布数字员工对话（服务层校验）。路径置于 {@code /api/digital-employees} 下，
 * 与既有数字员工接口共存。</p>
 *
 * @author harness-admin
 * @since 2026-10-08
 */
@RestController
@RequestMapping("/api/digital-employees")
public class PreviewChatController {

    /** 预览对话服务 */
    private final PreviewChatService previewChatService;

    /**
     * 构造预览对话控制器。
     *
     * @param previewChatService 预览对话服务
     */
    public PreviewChatController(PreviewChatService previewChatService) {
        this.previewChatService = previewChatService;
    }

    /**
     * 对话发起/延续与答复：一次性返回数字员工回复。
     *
     * @param id      数字员工 ID
     * @param request 对话请求（message 必填，conversationId 可空）
     * @return 会话 ID 与回复
     */
    @PostMapping("/{id}/chat")
    public ApiResponse<ChatResponse> chat(@PathVariable Long id, @Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(previewChatService.chat(id, request));
    }

    /**
     * 分页查询数字员工的会话历史。
     *
     * @param id   数字员工 ID
     * @param page 页码
     * @param size 每页条数
     * @return 会话分页结果
     */
    @GetMapping("/{id}/conversations")
    public ApiResponse<PageResult<ConversationResponse>> listConversations(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.success(previewChatService.listConversations(id, page, size));
    }

    /**
     * 查询指定会话的全部消息。
     *
     * @param id             数字员工 ID
     * @param conversationId 会话 ID
     * @return 消息列表
     */
    @GetMapping("/{id}/conversations/{conversationId}/messages")
    public ApiResponse<List<MessageResponse>> listMessages(@PathVariable Long id,
                                                           @PathVariable Long conversationId) {
        return ApiResponse.success(previewChatService.listMessages(id, conversationId));
    }
}