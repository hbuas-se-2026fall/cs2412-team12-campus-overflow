package com.campusoverflow.bootstrap.infrastructure.tag;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 标签词表仓储。
 */
public interface SpringDataTagRepository extends JpaRepository<TagJpaEntity, String> {
}
