package com.harness.admin.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件说明：管理接口角色约束注解。
 *
 * <p>标注在 Controller 类或方法上，由 {@link AuthInterceptor} 校验当前用户必须为 ADMIN 角色，
 * 普通用户访问返回禁止访问。管理接口默认安全边界由拦截器 + 该注解共同保证。</p>
 *
 * @author harness-admin
 * @since 2026-10-06
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireAdmin {
}
