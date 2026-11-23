package com.campusoverflow.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 7 Step 2：基于 OpenAPI 契约的接口测试。
 *
 * <p>两件事：</p>
 * <ol>
 *   <li><b>契约完整性</b>：代码里实现的每个 HTTP 映射，都必须在 {@code docs/api/openapi.yaml} 里有记录
 *       （防止"改了接口忘了改文档"）；</li>
 *   <li><b>响应结构</b>：真实调用接口，按 openapi.yaml 的 schema 校验必填字段与类型。</li>
 * </ol>
 *
 * <p>选型说明：Lab 7 把 Pact 列为可选项。我们没有引入 Pact 的 broker 依赖，
 * 而是用同一份 OpenAPI 契约做"服务端自证"，并让前端用同一份契约生成 TypeScript 客户端——
 * 前后端仍然由一份契约驱动，只是不需要额外的 broker 进程。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiContractTest {

    private static JsonNode spec;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @BeforeAll
    static void loadSpec() throws Exception {
        Path path = Path.of("../docs/api/openapi.yaml");
        assertTrue(Files.exists(path), "找不到 docs/api/openapi.yaml");
        spec = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(path));
    }

    @Test
    @DisplayName("契约完整性：所有已实现的接口都写进了 openapi.yaml")
    void everyImplementedEndpointIsDocumented() {
        Set<String> implemented = handlerMapping.getHandlerMethods().keySet().stream()
                .flatMap(info -> info.getPathPatternsCondition() == null
                        ? java.util.stream.Stream.<String>empty()
                        : info.getPathPatternsCondition().getPatternValues().stream())
                .filter(path -> !path.startsWith("/error"))
                .collect(Collectors.toSet());

        Set<String> documented = new HashSet<>();
        spec.path("paths").fieldNames().forEachRemaining(documented::add);

        Set<String> undocumented = implemented.stream()
                .filter(path -> !documented.contains(path))
                .collect(Collectors.toSet());

        assertTrue(undocumented.isEmpty(), "以下接口缺少 openapi.yaml 契约：" + undocumented);
        assertFalse(documented.isEmpty(), "openapi.yaml 没有任何 paths");
    }

    @Test
    @DisplayName("响应契约：注册/发问/列表/声誉的响应结构符合 schema")
    void responsesMatchSchema() throws Exception {
        String userId = registerAndVerify();

        var created = mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "askerId", userId,
                                "title", "契约测试用的问题标题",
                                "body", "契约测试用的问题正文。",
                                "tags", List.of("java")))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode question = objectMapper.readTree(created.getResponse().getContentAsString());
        assertMatches(question, "QuestionResponse", "QuestionResponse");

        var listResult = mockMvc.perform(get("/api/v1/questions")).andExpect(status().isOk()).andReturn();
        JsonNode page = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertMatches(page, "QuestionPageResponse", "QuestionPageResponse");

        var reputationResult = mockMvc.perform(get("/api/v1/users/{id}/reputation", userId))
                .andExpect(status().isOk()).andReturn();
        JsonNode reputation = objectMapper.readTree(reputationResult.getResponse().getContentAsString());
        assertMatches(reputation, "ReputationResponse", "ReputationResponse");
    }

    @Test
    @DisplayName("响应契约：错误响应符合 ErrorResponse schema")
    void errorResponseMatchesSchema() throws Exception {
        var result = mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "askerId", UUID.randomUUID().toString(),
                                "title", "不存在的提问者",
                                "body", "这条请求应该返回 404。",
                                "tags", List.of()))))
                .andExpect(status().isNotFound())
                .andReturn();

        JsonNode error = objectMapper.readTree(result.getResponse().getContentAsString());
        assertMatches(error, "ErrorResponse", "ErrorResponse");
    }

    private String registerAndVerify() throws Exception {
        var created = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "contract-" + UUID.randomUUID() + "@hbuas.edu.cn",
                                "nickname", "契约测试",
                                "password", "abc12345"))))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/users/{id}/email-verification", userId)).andExpect(status().isOk());
        return userId;
    }

    // ------------------------------------------------------------------ schema 校验

    private void assertMatches(JsonNode body, String schemaName, String path) {
        JsonNode schema = spec.at("/components/schemas/" + schemaName);
        assertFalse(schema.isMissingNode(), "openapi.yaml 缺少 schema：" + schemaName);
        validate(body, schema, path);
    }

    private void validate(JsonNode value, JsonNode schema, String path) {
        if (schema.has("$ref")) {
            schema = spec.at(schema.get("$ref").asText().substring(1));
        }
        if (value == null || value.isNull()) {
            assertTrue(schema.path("nullable").asBoolean(false), path + " 不应为 null");
            return;
        }
        String type = schema.path("type").asText();
        switch (type) {
            case "object" -> {
                assertTrue(value.isObject(), path + " 应为 object");
                schema.path("required").forEach(required -> assertTrue(
                        value.has(required.asText()), path + " 缺少必填字段：" + required.asText()));
                schema.path("properties").fields().forEachRemaining(entry -> {
                    if (value.has(entry.getKey())) {
                        validate(value.get(entry.getKey()), entry.getValue(), path + "." + entry.getKey());
                    }
                });
            }
            case "array" -> {
                assertTrue(value.isArray(), path + " 应为 array");
                JsonNode items = schema.path("items");
                for (int i = 0; i < value.size(); i++) {
                    validate(value.get(i), items, path + "[" + i + "]");
                }
            }
            case "integer" -> assertTrue(value.isIntegralNumber(), path + " 应为 integer，实际：" + value);
            case "number" -> assertTrue(value.isNumber(), path + " 应为 number");
            case "boolean" -> assertTrue(value.isBoolean(), path + " 应为 boolean");
            case "string" -> assertTrue(value.isTextual(), path + " 应为 string，实际：" + value);
            default -> {
                // 未声明 type 的 schema（如 $ref 已解析）不额外校验
            }
        }
    }
}
