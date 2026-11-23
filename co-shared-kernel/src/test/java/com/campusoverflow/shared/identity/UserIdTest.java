package com.campusoverflow.shared.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 跨上下文发布语言 UserId 的单元测试。
 */
class UserIdTest {

    @Test
    @DisplayName("newId 生成合法 UUID")
    void newIdIsUnique() {
        assertNotEquals(UserId.newId(), UserId.newId());
    }

    @Test
    @DisplayName("of 解析字符串并保持相等语义")
    void parsesString() {
        UUID uuid = UUID.randomUUID();
        UserId id = UserId.of(uuid.toString());

        assertEquals(uuid, id.value());
        assertEquals(uuid.toString(), id.asString());
        assertEquals(id, UserId.of(uuid.toString()));
    }

    @Test
    @DisplayName("非法输入抛 IllegalArgumentException")
    void rejectsInvalid() {
        assertThrows(IllegalArgumentException.class, () -> UserId.of("not-a-uuid"));
        assertThrows(IllegalArgumentException.class, () -> UserId.of(" "));
        assertThrows(NullPointerException.class, () -> new UserId(null));
    }
}
