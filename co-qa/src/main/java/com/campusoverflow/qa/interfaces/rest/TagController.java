package com.campusoverflow.qa.interfaces.rest;

import com.campusoverflow.shared.tag.Tag;
import com.campusoverflow.shared.tag.TagVocabulary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签词表接口（US-10）：前端下拉框与筛选器都从这里取，避免前后端各维护一份。
 */
@RestController
@RequestMapping("/api/v1/tags")
public class TagController {

    private final TagVocabulary tagVocabulary;

    public TagController(TagVocabulary tagVocabulary) {
        this.tagVocabulary = tagVocabulary;
    }

    @GetMapping
    public List<String> all() {
        return tagVocabulary.all().stream().map(Tag::value).sorted().toList();
    }
}
