package org.nicenomu.hairinjob.domain.job.repository;

import org.nicenomu.hairinjob.domain.job.model.entity.ScrapeSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScrapeSessionRepository extends JpaRepository<ScrapeSessionEntity, Long> {

    @Query("SELECT s FROM ScrapeSessionEntity s ORDER BY s.id DESC LIMIT 1")
    Optional<ScrapeSessionEntity> findLatestSession();

    @Query("SELECT MAX(s.id) FROM ScrapeSessionEntity s")
    Optional<Long> findLatestSessionId();
}
