package com.campusoverflow.qa.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/**
 * 问题-标签派生索引（US-10 按标签筛选）。
 *
 * <p>它不是聚合的一部分，只是 {@code question.tags} 的读模型投影：
 * 写入时由仓储同步，随时可以全量重建。</p>
 */
@Entity
@Table(name = "question_tag")
@IdClass(QuestionTagJpaEntity.QuestionTagId.class)
public class QuestionTagJpaEntity {

    @Id
    @Column(name = "question_id", length = 36, nullable = false)
    private String questionId;

    @Id
    @Column(name = "tag", length = 20, nullable = false)
    private String tag;

    protected QuestionTagJpaEntity() {
        // for JPA
    }

    public QuestionTagJpaEntity(String questionId, String tag) {
        this.questionId = questionId;
        this.tag = tag;
    }

    public String getQuestionId() {
        return questionId;
    }

    public String getTag() {
        return tag;
    }

    /** 复合主键。 */
    public static class QuestionTagId implements Serializable {

        private String questionId;
        private String tag;

        public QuestionTagId() {
        }

        public QuestionTagId(String questionId, String tag) {
            this.questionId = questionId;
            this.tag = tag;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof QuestionTagId that)) {
                return false;
            }
            return Objects.equals(questionId, that.questionId) && Objects.equals(tag, that.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(questionId, tag);
        }
    }
}
