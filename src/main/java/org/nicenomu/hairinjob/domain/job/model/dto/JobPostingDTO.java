package org.nicenomu.hairinjob.domain.job.model.dto;

import lombok.Builder;
import lombok.Getter;
import org.nicenomu.hairinjob.domain.job.model.entity.JobPostingEntity;
import org.nicenomu.hairinjob.domain.job.model.entity.ScrapeSessionEntity;

import java.util.List;

@Getter
@Builder
public class JobPostingDTO {

    private String idx;
    private String companyName;
    private String title;
    private String detailUrl;
    private String positions;
    private String location;
    private String salary;
    private String gender;
    private String deadline;
    private String grade;
    private String priorityBadge;
    private List<String> specialBadges;
    private String phone;
    private String companyAddress;
    private String companyPhone;
    private String recruiterName;
    private String recruiterContact;
    private Integer pageNumber;
    private Integer cookieSet;

    public JobPostingEntity toEntity(ScrapeSessionEntity session) {
        return JobPostingEntity.builder()
                .idx(idx)
                .companyName(companyName)
                .title(title)
                .detailUrl(detailUrl)
                .positions(positions)
                .location(location)
                .salary(salary)
                .gender(gender)
                .deadline(deadline)
                .grade(grade)
                .priorityBadge(priorityBadge)
                .specialBadges(specialBadges)
                .phone(phone)
                .companyAddress(companyAddress)
                .companyPhone(companyPhone)
                .recruiterName(recruiterName)
                .recruiterContact(recruiterContact)
                .pageNumber(pageNumber)
                .cookieSet(cookieSet)
                .session(session)
                .build();
    }

    public static JobPostingDTO fromEntity(JobPostingEntity entity) {
        return JobPostingDTO.builder()
                .idx(entity.getIdx())
                .companyName(entity.getCompanyName())
                .title(entity.getTitle())
                .detailUrl(entity.getDetailUrl())
                .positions(entity.getPositions())
                .location(entity.getLocation())
                .salary(entity.getSalary())
                .gender(entity.getGender())
                .deadline(entity.getDeadline())
                .grade(entity.getGrade())
                .priorityBadge(entity.getPriorityBadge())
                .specialBadges(entity.getSpecialBadges())
                .phone(entity.getPhone())
                .companyAddress(entity.getCompanyAddress())
                .companyPhone(entity.getCompanyPhone())
                .recruiterName(entity.getRecruiterName())
                .recruiterContact(entity.getRecruiterContact())
                .pageNumber(entity.getPageNumber())
                .cookieSet(entity.getCookieSet())
                .build();
    }
}
