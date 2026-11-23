package com.campusoverflow.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 真实 MySQL 集成测试（CI 用 GitHub Actions 的 service 容器）。
 *
 * <p>为什么不用 Testcontainers 跑 CI：本仓库的 Actions runner 上，
 * Testcontainers 1.21.3 探测不到 Docker（`Could not find a valid Docker environment`），
 * 用例会被跳过——那就等于"没验证"。改成"CI 起一个 MySQL service 容器，测试通过环境变量连它"，
 * 保证每次 CI 都真实执行 Flyway 迁移 + 真实写库断言。</p>
 *
 * <p>本地没有 Docker、也没有设置 {@code IT_MYSQL_URL} 时自动跳过，不影响 {@code mvn verify}。</p>
 *
 * <p>断言的内容与 {@link MySqlContainerIntegrationTest} 相同：
 * 注册 → 发问 → 回答 → 采纳 → 投票，然后直接查库确认
 * {@code question.vote_count=1}、{@code answer.accepted=1}、声誉明细 2 条。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "IT_MYSQL_URL", matches = ".+")
class MySqlServiceContainerIntegrationTest {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("IT_MYSQL_URL"));
        registry.add("spring.datasource.username", () -> envOrDefault("IT_MYSQL_USER", "campus"));
        registry.add("spring.datasource.password", () -> envOrDefault("IT_MYSQL_PASSWORD", "campus"));
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
        String askerId = registerAndVerify("ci-asker");
        String answererId = registerAndVerify("ci-answerer");

        String questionId = createQuestion(askerId, "CI 真实 MySQL 集成测试的问题标题", "java");
        String answerId = submitAnswer(questionId, answererId);

        mockMvc.perform(post("/api/v1/questions/{id}/acceptance", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answerId", answerId, "operatorId", askerId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedAnswerId").value(answerId));

        String voterId = registerAndVerify("ci-voter");
        mockMvc.perform(post("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "targetType", "QUESTION", "targetId", questionId,
                                "voterId", voterId, "value", "up"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetVoteCount").value(1));

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
        mockMvc.perform(post("/api/v1/users/{id}/email-verification", userId)).andExpect(status().isOk());
        return userId;
    }

    private String createQuestion(String askerId, String title, String tag) throws Exception {
        var created = mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "askerId", askerId, "title", title,
                                "body", "CI 真实 MySQL 集成测试的正文内容。", "tags", java.util.List.of(tag)))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    private String submitAnswer(String questionId, String authorId) throws Exception {
        var created = mockMvc.perform(post("/api/v1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId, "authorId", authorId,
                                "body", "CI 真实 MySQL 集成测试的回答正文。"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
