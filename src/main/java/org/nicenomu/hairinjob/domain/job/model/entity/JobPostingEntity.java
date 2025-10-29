package org.nicenomu.hairinjob.domain.job.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(
    name = "job_postings",
    indexes = {
        @Index(name = "idx_company_name", columnList = "company_name"),
        @Index(name = "idx_location", columnList = "location"),
        @Index(name = "idx_scraped_at", columnList = "scraped_at DESC"),
        @Index(name = "idx_updated_at", columnList = "updated_at DESC"),
        @Index(name = "idx_session_id", columnList = "session_id"),
        @Index(name = "idx_company_phone", columnList = "company_phone"),
        @Index(name = "idx_recruiter_contact", columnList = "recruiter_contact")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobPostingEntity extends BaseEntity {

    @Id
    @Column(name = "idx", length = 20)
    private String idx;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Column(name = "positions", columnDefinition = "TEXT")
    private String positions;

    @Column(name = "location", length = 100)
    private String location;

    @Column(name = "salary", length = 100)
    private String salary;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "deadline", length = 50)
    private String deadline;

    @Column(name = "grade", length = 20)
    private String grade;

    @Column(name = "priority_badge", length = 20)
    private String priorityBadge;

    @Type(JsonBinaryType.class)
    @Column(name = "special_badges", columnDefinition = "jsonb")
    private List<String> specialBadges;

    @Column(name = "phone", length = 20)
    private String phone;

    // 헤더 정보
    @Column(name = "company_address", columnDefinition = "TEXT")
    private String companyAddress;

    @Column(name = "company_phone", length = 20)
    private String companyPhone;

    @Column(name = "recruiter_name", length = 100)
    private String recruiterName;

    @Column(name = "recruiter_contact", length = 20)
    private String recruiterContact;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "cookie_set")
    private Integer cookieSet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private ScrapeSessionEntity session;

    @Builder
    public JobPostingEntity(
            String idx,
            String companyName,
            String title,
            String detailUrl,
            String positions,
            String location,
            String salary,
            String gender,
            String deadline,
            String grade,
            String priorityBadge,
            List<String> specialBadges,
            String phone,
            String companyAddress,
            String companyPhone,
            String recruiterName,
            String recruiterContact,
            Integer pageNumber,
            Integer cookieSet,
            ScrapeSessionEntity session
    ) {
        this.idx = idx;
        this.companyName = companyName;
        this.title = title;
        this.detailUrl = detailUrl;
        this.positions = positions;
        this.location = location;
        this.salary = salary;
        this.gender = gender;
        this.deadline = deadline;
        this.grade = grade;
        this.priorityBadge = priorityBadge;
        this.specialBadges = specialBadges;
        this.phone = phone;
        this.companyAddress = companyAddress;
        this.companyPhone = companyPhone;
        this.recruiterName = recruiterName;
        this.recruiterContact = recruiterContact;
        this.pageNumber = pageNumber;
        this.cookieSet = cookieSet;
        this.session = session;
    }

    public void updateDetailInfo(
            String phone,
            String companyAddress,
            String companyPhone,
            String recruiterName,
            String recruiterContact
    ) {
        this.phone = phone;
        this.companyAddress = companyAddress;
        this.companyPhone = companyPhone;
        this.recruiterName = recruiterName;
        this.recruiterContact = recruiterContact;
    }
}
