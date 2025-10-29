package org.nicenomu.hairinjob.domain.job.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PreUpdate;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Column(name = "scraped_at", nullable = false, updatable = false)
    private LocalDateTime scrapedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BaseEntity() {
        this.scrapedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
