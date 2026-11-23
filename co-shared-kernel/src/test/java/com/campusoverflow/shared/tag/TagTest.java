package com.campusoverflow.shared.tag;

import com.campusoverflow.shared.domain.InvariantViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 标签值对象与内存词表的单元测试（共享内核）。
 */
class TagTest {

    @Test
    @DisplayName("标签归一化：去空白 + 转小写")
    void normalizes() {
        assertEquals("spring-boot", Tag.of("  Spring-Boot ").value());
    }

    @Test
    @DisplayName("INV-SK-01：长度必须在 2–20 之间")
    void rejectsBadLength() {
        assertThrows(InvariantViolationException.class, () -> Tag.of("a"));
        assertThrows(InvariantViolationException.class, () -> Tag.of("x".repeat(21)));
    }

    @Test
    @DisplayName("INV-SK-01：只允许小写字母、数字、汉字与连字符")
    void rejectsIllegalCharacters() {
        assertThrows(InvariantViolationException.class, () -> Tag.of("hello world"));
        assertThrows(InvariantViolationException.class, () -> Tag.of("a_b"));
        assertThrows(InvariantViolationException.class, () -> Tag.of("   "));
    }

    @Test
    @DisplayName("汉字标签合法，toString 与 value 一致")
    void acceptsChinese() {
        Tag tag = Tag.of("校园生活");
        assertEquals("校园生活", tag.value());
        assertEquals("校园生活", tag.toString());
    }

    @Test
    @DisplayName("内存词表：登记后 contains 为真，all 返回去重集合")
    void inMemoryVocabularyRegisters() {
        InMemoryTagVocabulary vocabulary = new InMemoryTagVocabulary(Set.of("java"));

        assertTrue(vocabulary.contains(Tag.of("java")));
        assertFalse(vocabulary.contains(Tag.of("mysql")));

        vocabulary.registerIfAbsent(Tag.of("mysql"));

        assertTrue(vocabulary.contains(Tag.of("mysql")));
        assertEquals(2, vocabulary.all().size());
    }

    @Test
    @DisplayName("内存词表：空集合初始化不报错")
    void inMemoryVocabularyHandlesNull() {
        InMemoryTagVocabulary vocabulary = new InMemoryTagVocabulary(null);
        assertTrue(vocabulary.all().isEmpty());
    }
}
