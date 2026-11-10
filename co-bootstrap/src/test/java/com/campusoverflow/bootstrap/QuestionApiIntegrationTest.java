package com.campusoverflow.bootstrap;

import com.campusoverflow.qa.domain.model.QuestionId;
import com.campusoverflow.qa.domain.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 端到端接口测试（Lab 4 · Step 4）：注册 → 验证邮箱 → 发布问题 → 读回 → 落库。
 *
 * <p>用 MockMvc 走完整的 Spring MVC 链路（过滤器、参数校验、全局异常处理都在），
 * 数据库换成 H2 内存库，因此不需要本地 MySQL 也能跑。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class QuestionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("注册 → 验证邮箱 → 发布问题 → 读回，数据库里能查到同一条记录")
    void endToEndPostQuestion() throws Exception {
        String userId = registerAndVerify("student-" + UUID.randomUUID() + "@hbuas.edu.cn");

        MvcResult created = mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "askerId", userId,
                                "title", "Flyway 和 JPA 的初始化顺序应该怎么配？",
                                "body", "启动时 Hibernate 先跑，报错说表不存在。",
                                "tags", List.of("flyway", "spring-data-jpa")))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.askerId").value(userId))
                .andExpect(jsonPath("$.status").value("open"))
                .andExpect(jsonPath("$.voteCount").value(0))
                .andExpect(jsonPath("$.tags.length()").value(2))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String questionId = body.get("id").asText();

        // 读回：证明写进库里的东西能被重新查出来（不是内存里返回了一个对象）
        mockMvc.perform(get("/api/v1/questions/{id}", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Flyway 和 JPA 的初始化顺序应该怎么配？"))
                .andExpect(jsonPath("$.tags[0]").value("flyway"));

        assertTrue(questionRepository.findById(QuestionId.of(questionId)).isPresent(),
                "问题应已持久化到数据库");
    }

    @Test
    @DisplayName("未验证邮箱的账号发布问题返回 400 + INV-U-02")
    void unverifiedUserCannotPost() throws Exception {
        String userId = register("pending-" + UUID.randomUUID() + "@hbuas.edu.cn");

        mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionJson(userId, "未验证账号能不能发问题？", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV-U-02"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("标签不在共享词表 SK-01 中返回 400 + INV-Q-02")
    void unknownTagRejected() throws Exception {
        String userId = registerAndVerify("tag-" + UUID.randomUUID() + "@hbuas.edu.cn");

        mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionJson(userId, "这个问题带了一个词表外的标签", List.of("我自己造的词"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV-Q-02"));
    }

    @Test
    @DisplayName("标题过短在接口层就被拦下，返回字段级错误 details")
    void shortTitleRejected() throws Exception {
        String userId = registerAndVerify("title-" + UUID.randomUUID() + "@hbuas.edu.cn");

        mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionJson(userId, "短", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("title"));
    }

    @Test
    @DisplayName("不存在的提问者返回 404")
    void unknownAskerRejected() throws Exception {
        mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(questionJson(UUID.randomUUID().toString(), "提问者根本不存在的问题", List.of())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    @DisplayName("同一邮箱重复注册返回 409 + INV-U-01")
    void duplicateEmailRejected() throws Exception {
        String email = "dup-" + UUID.randomUUID() + "@hbuas.edu.cn";
        register(email);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "nickname", "重复注册", "password", "abc12345"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INV-U-01"));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "nickname", "集成测试同学", "password", "abc12345"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndVerify(String email) throws Exception {
        String userId = register(email);
        mockMvc.perform(post("/api/v1/users/{id}/email-verification", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
        return userId;
    }

    private String questionJson(String askerId, String title, List<String> tags) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "askerId", askerId,
                "title", title,
                "body", "这是端到端测试用的正文，长度足够。",
                "tags", tags));
    }
}
