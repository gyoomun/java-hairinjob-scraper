package org.nicenomu.hairinjob.domain.job.repository;

import org.nicenomu.hairinjob.domain.job.model.entity.JobPostingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPostingEntity, String> {

    @Query("""
        SELECT j FROM JobPostingEntity j
        WHERE j.session.id = :sessionId
        AND j.detailUrl IS NOT NULL
        ORDER BY j.pageNumber ASC, j.idx ASC
        """)
    List<JobPostingEntity> findAllDetailUrlsBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
        SELECT j FROM JobPostingEntity j
        WHERE j.session.id = (SELECT MAX(s.id) FROM ScrapeSessionEntity s)
        AND j.detailUrl IS NOT NULL
        ORDER BY j.pageNumber ASC, j.idx ASC
        """)
    List<JobPostingEntity> findAllDetailUrlsFromLatestSession();

    boolean existsByIdx(String idx);

    List<JobPostingEntity> findBySessionId(Long sessionId);

    @Query("SELECT COUNT(j) FROM JobPostingEntity j WHERE j.phone IS NOT NULL")
    long countWithPhone();

    @Query("SELECT COUNT(j) FROM JobPostingEntity j WHERE j.companyPhone IS NOT NULL")
    long countWithCompanyPhone();

    @Query("SELECT COUNT(j) FROM JobPostingEntity j WHERE j.recruiterContact IS NOT NULL")
    long countWithRecruiterContact();
}
