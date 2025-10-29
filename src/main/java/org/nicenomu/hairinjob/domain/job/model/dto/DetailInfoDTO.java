package org.nicenomu.hairinjob.domain.job.model.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DetailInfoDTO {

    private String idx;
    private String phone;
    private String companyAddress;
    private String companyPhone;
    private String recruiterName;
    private String recruiterContact;
}
