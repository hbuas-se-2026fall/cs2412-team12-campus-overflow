package com.campusoverflow.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Lab 7 Step 2：基于 Testcontainers 的真实 MySQL 集成测试（Controller → Service → Repository → DB）。
 *
 * <p>与 H2 测试的区别：</p>
 * <ul>
 *   <li>真实执行 Flyway 迁移（V1 + V2），并开启 {@code ddl-auto=validate}，
 *       能发现"实体映射与迁移脚本不一致"这种只有真库才暴露的问题；</li>
 *   <li>断言最终写进了数据库，而不是只相信接口返回值。</li>
 * </ul>
 *
 * <p>没有 Docker 的环境会自动跳过（{@code disabledWithoutDocker = true}），
 * 因此本地 {@code mvn verify} 仍然全绿；CI 的 ubuntu runner 有 Docker，会真实执行。</p>
 */
@Tag("testcontainers")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class MySqlContainerIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("campus_overflow")
            .withUsername("campus")
            .withPassword("campus");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void fullFlowPersistsIntoRealMySql() throws Exception {
        String askerId = registerAndVerify("tc-asker");
        String answererId = registerAndVerify("tc-answerer");

        String questionId = createQuestion(askerId, "Testcontainers 集成测试的问题标题", "java");
        String answerId = submitAnswer(questionId, answererId);

        mockMvc.perform(post("/api/v1/questions/{id}/acceptance", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answerId", answerId, "operatorId", askerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedAnswerId").value(answerId));

        // 票数投影：第三方投票者
        String voterId = registerAndVerify("tc-voter");
        mockMvc.perform(post("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "QUESTION", "targetId", questionId,
                                "voterId", voterId, "value", "up"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetVoteCount").value(1));

        // 直接查真库，验证"接口返回 200"不等于"数据真的写进去了"
        Integer questionRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question WHERE id = ? AND vote_count = 1", Integer.class, questionId);
        Integer answerRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM answer WHERE id = ? AND accepted = 1", Integer.class, answerId);
        Integer voteRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vote WHERE target_id = ? AND vote_value = 'up'", Integer.class, questionId);
        Integer reputationRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reputation_entry WHERE user_id = ?", Integer.class, answererId);

        assertEquals(1, questionRows);
        assertEquals(1, answerRows);
        assertEquals(1, voteRows);
        assertEquals(2, reputationRows, "回答 +5 与被采纳 +15 都应有明细");
    }

    private String registerAndVerify(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@hbuas.edu.cn";
        var created = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "nickname", prefix, "password", "abc12345"))))
                .andExpect(status().isCreated())
                .andReturn();
        String userId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/v1/users/{id}/email-verification", userId))
                .andExpect(status().isOk());
        return userId;
    }

    private String createQuestion(String askerId, String title, String tag) throws Exception {
        var created = mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "askerId", askerId, "title", title,
                                "body", "Testcontainers 集成测试的正文内容。", "tags", java.util.List.of(tag)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    private String submitAnswer(String questionId, String authorId) throws Exception {
        var created = mockMvc.perform(post("/api/v1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId, "authorId", authorId,
                                "body", "Testcontainers 集成测试的回答正文。"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    @SuppressWarnings("unused")
    private JsonNode getJson(String path) throws Exception {
        var result = mockMvc.perform(get(path)).andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
