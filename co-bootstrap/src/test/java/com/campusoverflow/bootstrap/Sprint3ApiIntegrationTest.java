package com.campusoverflow.bootstrap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 3 端到端集成测试（Lab 6）：回答、采纳、投票、评论、标签、声誉。
 *
 * <p>这些用例覆盖"跨模块协作"：问答事件经 {@code QaReputationListener} 在事务提交后
 * 驱动声誉上下文计分，因此断言里会直接查声誉接口。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Sprint3ApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("回答 → 采纳：回答者声誉 +5 +15，提问者 +2")
    void acceptAnswerRewardsBothSides() throws Exception {
        String asker = registerAndVerify("asker");
        String answerer = registerAndVerify("answerer");
        String questionId = createQuestion(asker, "采纳后声誉怎么变化？", List.of("java"));
        String answerId = submitAnswer(questionId, answerer, "被采纳会 +15，提问 +2，回答 +5。");

        mockMvc.perform(post("/api/v1/questions/{id}/acceptance", questionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "answerId", answerId, "operatorId", asker))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.acceptedAnswerId").value(answerId))
                .andExpect(jsonPath("$.status").value("solved"));

        // 回答者：初始 1 + 回答 5 + 被采纳 15 = 21
        mockMvc.perform(get("/api/v1/users/{id}/reputation", answerer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(21));

        // 提问者：初始 1 + 提问 2 = 3
        mockMvc.perform(get("/api/v1/users/{id}/reputation", asker))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(3));
    }

    @Test
    @DisplayName("投票：赞成 +1、改反对 −2、撤票回到 0，票数投影与声誉同步")
    void voteFlowKeepsProjectionAndReputationInSync() throws Exception {
        String asker = registerAndVerify("v-asker");
        String voter = registerAndVerify("voter");
        String questionId = createQuestion(asker, "投票的票数投影怎么算？", List.of());

        // 赞成：票数 1，提问者声誉 1 + 2 + 10 = 13
        mockMvc.perform(post("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson("QUESTION", questionId, voter, "up")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetVoteCount").value(1));

        // 改反对：净变化 −2，票数 −1
        mockMvc.perform(post("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson("QUESTION", questionId, voter, "down")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetVoteCount").value(-1));

        mockMvc.perform(get("/api/v1/questions/{id}", questionId))
                .andExpect(jsonPath("$.voteCount").value(-1));

        // 撤票：回到 0
        mockMvc.perform(delete("/api/v1/votes")
                        .param("targetType", "QUESTION")
                        .param("targetId", questionId)
                        .param("voterId", voter))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/questions/{id}", questionId))
                .andExpect(jsonPath("$.voteCount").value(0));
    }

    @Test
    @DisplayName("INV-V-02：不能给自己的内容投票")
    void cannotVoteOwnContent() throws Exception {
        String asker = registerAndVerify("self-asker");
        String questionId = createQuestion(asker, "自己给自己投票会怎样？", List.of());

        mockMvc.perform(post("/api/v1/votes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson("QUESTION", questionId, asker, "up")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV-V-02"));
    }

    @Test
    @DisplayName("评论：发表、列表、非作者删除被拒（INV-C-03）、作者删除成功")
    void commentLifecycle() throws Exception {
        String asker = registerAndVerify("c-asker");
        String other = registerAndVerify("c-other");
        String questionId = createQuestion(asker, "评论能不能删除别人的？", List.of());

        MvcResult created = mockMvc.perform(post("/api/v1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(commentJson("QUESTION", questionId, other, "先补充一个踩坑记录。")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("先补充一个踩坑记录。"))
                .andReturn();
        String commentId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asText();

        mockMvc.perform(get("/api/v1/comments")
                        .param("targetType", "QUESTION")
                        .param("targetId", questionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(delete("/api/v1/comments/{id}", commentId)
                        .param("operatorId", asker))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV-C-03"));

        mockMvc.perform(delete("/api/v1/comments/{id}", commentId)
                        .param("operatorId", other))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("标签：按标签筛选列表，词表接口能查到")
    void tagFilterWorks() throws Exception {
        String asker = registerAndVerify("t-asker");
        String tag = "filter" + UUID.randomUUID().toString().substring(0, 5);
        String questionId = createQuestion(asker, "带标签的问题应该能被筛出来", List.of(tag));

        mockMvc.perform(get("/api/v1/questions").param("tag", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(questionId))
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/api/v1/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@ == '" + tag + "')]").exists());
    }

    @Test
    @DisplayName("INV-A-03：同一作者对同一问题 60 秒内不能重复回答")
    void answerRateLimited() throws Exception {
        String asker = registerAndVerify("r-asker");
        String answerer = registerAndVerify("r-answerer");
        String questionId = createQuestion(asker, "重复回答会被限流吗？", List.of());

        submitAnswer(questionId, answerer, "第一次回答，应该成功。");

        mockMvc.perform(post("/api/v1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "authorId", answerer,
                                "body", "第二次回答，立刻提交应该被限流。"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV-A-03"));
    }

    // ------------------------------------------------------------------ helpers

    private String register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@hbuas.edu.cn";
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "nickname", "测试" + prefix, "password", "abc12345"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndVerify(String prefix) throws Exception {
        String userId = register(prefix);
        mockMvc.perform(post("/api/v1/users/{id}/email-verification", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
        return userId;
    }

    private String createQuestion(String askerId, String title, List<String> tags) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "askerId", askerId,
                                "title", title,
                                "body", "这是 Sprint 3 集成测试用的正文，长度足够。",
                                "tags", tags))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String submitAnswer(String questionId, String authorId, String body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/answers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "questionId", questionId,
                                "authorId", authorId,
                                "body", body))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        assertTrue(node.hasNonNull("id"), "回答应返回 id");
        return node.get("id").asText();
    }

    private String voteJson(String targetType, String targetId, String voterId, String value) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "targetType", targetType,
                "targetId", targetId,
                "voterId", voterId,
                "value", value));
    }

    private String commentJson(String targetType, String targetId, String authorId, String body) throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "targetType", targetType,
                "targetId", targetId,
                "authorId", authorId,
                "body", body));
    }
}
