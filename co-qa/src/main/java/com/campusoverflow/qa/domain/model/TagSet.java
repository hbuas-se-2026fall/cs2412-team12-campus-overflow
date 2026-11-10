package com.campusoverflow.qa.domain.model;

import com.campusoverflow.shared.domain.InvariantViolationException;
import com.campusoverflow.shared.tag.Tag;
import com.campusoverflow.shared.tag.TagVocabulary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 标签集合值对象（{@code INV-Q-02}）：最多 5 个、不可重复，且每个标签必须存在于共享内核词表 SK-01。
 *
 * <p>允许为空集合：Lab 2 的 Sprint 1 里提问不带标签（US-10 在 Sprint 3）。</p>
 */
public record TagSet(List<Tag> values) {

    public static final int MAX_TAGS = 5;
    public static final String INVARIANT_CODE = QuestionInvariants.TAG_SET;

    public TagSet {
        List<Tag> copy = values == null ? List.of() : List.copyOf(values);
        if (copy.size() > MAX_TAGS) {
            throw new InvariantViolationException(INVARIANT_CODE, "最多只能打 " + MAX_TAGS + " 个标签，当前 " + copy.size() + " 个");
        }
        Set<Tag> distinct = new LinkedHashSet<>(copy);
        if (distinct.size() != copy.size()) {
            throw new InvariantViolationException(INVARIANT_CODE, "标签不允许重复");
        }
        values = copy;
    }

    public static TagSet empty() {
        return new TagSet(List.of());
    }

    /**
     * 从原始字符串构造，并校验标签必须在 SK-01 词表内（统一术语，避免同义词分裂检索口径）。
     */
    public static TagSet of(Collection<String> rawTags, TagVocabulary vocabulary) {
        if (rawTags == null || rawTags.isEmpty()) {
            return empty();
        }
        List<Tag> tags = new ArrayList<>(rawTags.size());
        Set<Tag> seen = new LinkedHashSet<>();
        for (String raw : rawTags) {
            Tag tag = Tag.of(raw);
            if (!vocabulary.contains(tag)) {
                throw new InvariantViolationException(INVARIANT_CODE,
                        "标签「" + tag.value() + "」不在共享词表 SK-01 中，请先申请新增词条");
            }
            if (!seen.add(tag)) {
                throw new InvariantViolationException(INVARIANT_CODE, "标签不允许重复：" + tag.value());
            }
            tags.add(tag);
        }
        return new TagSet(tags);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    /** 持久化用：逗号分隔（标签语法本身不允许逗号，见 {@link Tag}）。 */
    public String asCsv() {
        return String.join(",", values.stream().map(Tag::value).toList());
    }

    public static TagSet fromCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return empty();
        }
        List<Tag> tags = new ArrayList<>();
        for (String part : csv.split(",")) {
            if (!part.isBlank()) {
                tags.add(Tag.of(part));
            }
        }
        return new TagSet(tags);
    }
}
