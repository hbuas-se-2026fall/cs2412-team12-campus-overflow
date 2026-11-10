package com.campusoverflow.bootstrap.interfaces.error;

import java.time.Instant;
import java.util.List;

/**
 * 统一错误响应：所有 4xx/5xx 都长这样，前端按 {@code code} 分支，不解析中文 message。
 *
 * @param code      稳定错误码（不变式编号优先，如 INV-Q-01）
 * @param message   给用户看的中文说明
 * @param traceId   与日志、响应头 X-Trace-Id 一致，便于反查
 * @param timestamp 服务端时间
 * @param path      请求路径
 * @param details   字段级错误（校验失败时非空）
 */
public record ErrorResponse(
        String code,
        String message,
        String traceId,
        Instant timestamp,
        String path,
        List<Detail> details
) {

    public record Detail(String field, String message) {
    }

    public static ErrorResponse of(String code, String message, String traceId, Instant timestamp,
                                   String path, List<Detail> details) {
        return new ErrorResponse(code, message, traceId, timestamp, path, details == null ? List.of() : details);
    }
}
