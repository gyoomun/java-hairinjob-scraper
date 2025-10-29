package org.nicenomu.hairinjob.domain.job.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.nicenomu.hairinjob.common.util.GlobalConstants;
import org.nicenomu.hairinjob.domain.job.model.dto.DetailInfoDTO;
import org.nicenomu.hairinjob.domain.job.model.dto.JobPostingDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class HtmlParserService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("010[-\\.\\s]?\\d{3,4}[-\\.\\s]?\\d{4}");
    private static final Pattern NUMBER_HYPHEN_PATTERN = Pattern.compile("[^0-9-]");

    public List<JobPostingDTO> parseJobListings(String html, Integer pageNumber, Integer cookieSet) {
        Document doc = Jsoup.parse(html);
        List<JobPostingDTO> jobs = new ArrayList<>();

        Elements jobRows = doc.select("tr.line01");

        for (Element row : jobRows) {
            try {
                JobPostingDTO.JobPostingDTOBuilder builder = JobPostingDTO.builder()
                        .pageNumber(pageNumber)
                        .cookieSet(cookieSet);

                // idx 추출
                Element scrapLink = row.selectFirst("a[href*=scrap_check]");
                if (scrapLink != null) {
                    String href = scrapLink.attr("href");
                    String[] parts = href.split("'");
                    if (parts.length > 3) {
                        builder.idx(parts[3]);
                    }
                }

                // 회사명 추출
                Element companySpan = row.selectFirst("span.span0101");
                if (companySpan != null) {
                    String companyName = companySpan.text()
                            .replace("찜하기", "")
                            .trim();
                    builder.companyName(companyName);
                }

                // 공고 제목 및 상세 URL 추출
                Elements titleLinks = row.select("a[title=상세채용정보보기]");
                for (Element link : titleLinks) {
                    String href = link.attr("href");
                    if (href.contains("s01_v.php") && link.hasAttr("style") && !link.text().isEmpty()) {
                        builder.title(link.text().trim());
                        builder.detailUrl(href.startsWith("http") ? href : GlobalConstants.BASE_URL + href);
                        break;
                    }
                }

                // 모집 직종 추출
                Element positionSpan = row.selectFirst("span.span0301");
                if (positionSpan != null) {
                    builder.positions(positionSpan.text().trim());
                }

                // 근무지역, 급여, 성별, 마감일 추출
                Element infoSpan = row.selectFirst("span.span0302");
                if (infoSpan != null) {
                    Element paySpan = infoSpan.selectFirst("span.pay");
                    String payText = paySpan != null ? paySpan.text().trim() : "";

                    String infoText = infoSpan.text().trim();
                    String[] parts = infoText.split("　");

                    if (parts.length >= 1) {
                        builder.location(parts[0].replace("#", "").trim());
                    }
                    if (parts.length >= 2) {
                        String salaryPart = parts[1].replace("#", "").trim();
                        if (!payText.isEmpty()) {
                            builder.salary(payText + salaryPart.replace(payText, ""));
                        } else {
                            builder.salary(salaryPart);
                        }
                    }
                    if (parts.length >= 3) {
                        builder.gender(parts[2].replace("#", "").trim());
                    }
                    if (parts.length >= 4) {
                        builder.deadline(parts[3].replace("#", "").trim());
                    }
                }

                // 등급 추출 (VVIP, VIP 등)
                Element gradeSpan = row.select("span[class*=css_]").first();
                if (gradeSpan != null) {
                    builder.grade(gradeSpan.text().trim());
                }

                // 우선 배지 확인
                Element priorityBadge = row.selectFirst("span.icon_top");
                if (priorityBadge != null) {
                    builder.priorityBadge(priorityBadge.text().trim());
                }

                // 특수 배지 확인
                List<String> specialBadges = new ArrayList<>();
                for (String badgeClass : List.of("icon_mini21", "icon_mini23")) {
                    Element badge = row.selectFirst("span." + badgeClass);
                    if (badge != null) {
                        specialBadges.add(badge.text().trim());
                    }
                }
                if (!specialBadges.isEmpty()) {
                    builder.specialBadges(specialBadges);
                }

                jobs.add(builder.build());

            } catch (Exception e) {
                log.warn("항목 파싱 중 오류", e);
            }
        }

        return jobs;
    }

    public DetailInfoDTO parseDetailPage(String html, String idx) {
        Document doc = Jsoup.parse(html);
        DetailInfoDTO.DetailInfoDTOBuilder builder = DetailInfoDTO.builder().idx(idx);

        // 헤더 정보 추출
        Set<String> foundFields = new HashSet<>();
        Elements leftItems = doc.select("li.left");

        for (Element leftLi : leftItems) {
            if (foundFields.size() >= 4) {
                break;
            }

            String label = leftLi.text().trim();
            if (foundFields.contains(label)) {
                continue;
            }

            Element rightLi = leftLi.nextElementSibling();
            if (rightLi == null || !rightLi.hasClass("right")) {
                continue;
            }

            switch (label) {
                case "업체주소":
                    builder.companyAddress(rightLi.text().trim());
                    foundFields.add(label);
                    break;
                case "전화번호":
                    Element phoneSpan = rightLi.selectFirst("span");
                    if (phoneSpan != null) {
                        builder.companyPhone(phoneSpan.text().trim());
                        foundFields.add(label);
                    }
                    break;
                case "채용담당자":
                    Element nameSpan = rightLi.selectFirst("span");
                    if (nameSpan != null) {
                        builder.recruiterName(nameSpan.text().trim());
                        foundFields.add(label);
                    }
                    break;
                case "담당자연락처":
                    String text = rightLi.text().trim();
                    if (!text.isEmpty()) {
                        String[] textParts = text.split("\\s+");
                        if (textParts.length > 0) {
                            String cleaned = NUMBER_HYPHEN_PATTERN.matcher(textParts[0]).replaceAll("");
                            if (!cleaned.isEmpty()) {
                                builder.recruiterContact(cleaned);
                                foundFields.add(label);
                            }
                        }
                    }
                    break;
            }
        }

        // 상세내용에서 전화번호 추출
        Elements textboxes = doc.select("div.bottom_textbox");
        for (Element textbox : textboxes) {
            Element prevSibling = textbox.previousElementSibling();
            if (prevSibling != null && prevSibling.text().contains("상세내용")) {
                String detailContent = textbox.text().trim();
                Matcher matcher = PHONE_PATTERN.matcher(detailContent);
                if (matcher.find()) {
                    builder.phone(matcher.group());
                    break;
                }
            }
        }

        return builder.build();
    }

    public Integer getLastPageNumber(String html) {
        Document doc = Jsoup.parse(html);

        Element navImg = doc.selectFirst("img[src=/images/newhair/nav_l.gif]");
        if (navImg != null) {
            Element parentLink = navImg.parent();
            if (parentLink != null) {
                String href = parentLink.attr("href");
                Pattern pattern = Pattern.compile("page=(\\d+)");
                Matcher matcher = pattern.matcher(href);
                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }
            }
        }

        return 1;
    }
}
