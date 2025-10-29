package org.nicenomu.hairinjob.domain.job.model.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "scrape_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScrapeSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "total_jobs")
    private Integer totalJobs;

    @Column(name = "new_jobs")
    private Integer newJobs;

    @Column(name = "updated_jobs")
    private Integer updatedJobs;

    @Column(name = "num_cookies")
    private Integer numCookies;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobPostingEntity> jobPostings = new ArrayList<>();

    @Builder
    public ScrapeSessionEntity(
            LocalDateTime startedAt,
            Integer totalPages,
            Integer numCookies,
            String status
    ) {
        this.startedAt = startedAt != null ? startedAt : LocalDateTime.now();
        this.totalPages = totalPages;
        this.numCookies = numCookies;
        this.status = status != null ? status : "RUNNING";
    }

    public void complete(Integer totalJobs, Integer newJobs, Integer updatedJobs) {
        this.completedAt = LocalDateTime.now();
        this.totalJobs = totalJobs;
        this.newJobs = newJobs;
        this.updatedJobs = updatedJobs;
        this.status = "COMPLETED";
    }

    public void fail(String errorMessage) {
        this.completedAt = LocalDateTime.now();
        this.status = "FAILED";
        this.errorMessage = errorMessage;
    }
}
