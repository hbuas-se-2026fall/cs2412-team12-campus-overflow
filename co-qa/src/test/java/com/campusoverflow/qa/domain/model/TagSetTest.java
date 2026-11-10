package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.tag.InMemoryTagVocabulary;
import com.campusoverflow.shared.tag.TagVocabulary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TagSet 与共享内核词表 SK-01 的协作测试（INV-Q-02）。
 */
class TagSetTest {

    private static final TagVocabulary VOCABULARY =
            new InMemoryTagVocabulary(Set.of("java", "spring-boot", "mysql", "flyway", "ddd"));

    @Test
    @DisplayName("标签按小写归一化后去重校验")
    void normalizesAndDeduplicates() {
        TagSet tags = TagSet.of(List.of("Java", "MYSQL"), VOCABULARY);

        assertEquals(List.of("java", "mysql"), tags.values().stream().map(t -> t.value()).toList());
    }

    @Test
    @DisplayName("INV-Q-02：不在 SK-01 词表里的标签被拒绝")
    void rejectsUnknownTag() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> TagSet.of(List.of("我随便起的标签"), VOCABULARY));
        assertEquals(QuestionInvariants.TAG_SET, ex.code());
    }

    @Test
    @DisplayName("INV-Q-02：标签重复被拒绝")
    void rejectsDuplicateTag() {
        InvariantViolationException ex = assertThrows(InvariantViolationException.class,
                () -> TagSet.of(List.of("java", "JAVA"), VOCABULARY));
        assertEquals(QuestionInvariants.TAG_SET, ex.code());
    }

    @Test
    @DisplayName("空标签集合是允许的（Sprint 1 提问可以不带标签）")
    void emptyIsAllowed() {
        assertTrue(TagSet.of(null, VOCABULARY).isEmpty());
        assertTrue(TagSet.of(List.of(), VOCABULARY).isEmpty());
    }

    @Test
    @DisplayName("持久化用逗号分隔，可无损往返")
    void csvRoundTrip() {
        TagSet tags = TagSet.of(List.of("java", "spring-boot"), VOCABULARY);

        assertEquals("java,spring-boot", tags.asCsv());
        assertEquals(tags, TagSet.fromCsv(tags.asCsv()));
    }
}
