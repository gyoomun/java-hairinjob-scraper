package org.nicenomu.hairinjob.domain.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.common.exception.GlobalExceptionHandler;
import org.nicenomu.hairinjob.common.config.ScraperProperties;
import org.nicenomu.hairinjob.common.util.GlobalConstants;
import org.nicenomu.hairinjob.domain.job.model.dto.DetailInfoDTO;
import org.nicenomu.hairinjob.domain.job.model.entity.JobPostingEntity;
import org.nicenomu.hairinjob.domain.job.repository.JobPostingRepository;
import org.nicenomu.hairinjob.infrastructure.browser.PlaywrightManager;
import org.nicenomu.hairinjob.infrastructure.http.HttpClientService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class DetailScrapingService {

    private final JobPostingRepository jobPostingRepository;
    private final HtmlParserService htmlParserService;
    private final PlaywrightManager playwrightManager;
    private final ScraperProperties scraperProperties;
    private final Random random = new Random();

    @Transactional
    public void scrapeAllDetailPages() {
        List<JobPostingEntity> jobs = jobPostingRepository.findAllDetailUrlsFromLatestSession();

        log.info("\n상세페이지 스크래핑 시작: 총 {}개", jobs.size());

        Instant lastCookieRefresh = Instant.now();
        Duration cookieRefreshInterval = Duration.ofMinutes(
                scraperProperties.getCookieRefreshIntervalMinutes()
        );

        for (int i = 0; i < jobs.size(); i++) {
            JobPostingEntity job = jobs.get(i);

            log.info("[{}/{}] idx={} - {} 스크래핑 중...", i + 1, jobs.size(), job.getIdx(), job.getTitle());

            try {
                // 쿠키 갱신 체크 (5분마다)
                if (Duration.between(lastCookieRefresh, Instant.now()).compareTo(cookieRefreshInterval) > 0) {
                    log.info("\n🔄 쿠키 갱신 필요 ({}분 경과)", scraperProperties.getCookieRefreshIntervalMinutes());
                    log.info("⏸️  상세페이지 스크래핑 일시 중지 - 로그인 유지 쿠키 보존 절차 시작");

                    // PlaywrightManager에서 쿠키 재갱신
                    playwrightManager.refreshCookies();

                    lastCookieRefresh = Instant.now();
                    log.info("▶️  상세페이지 스크래핑 재개");
                }

                // Playwright APIRequest로 HTML 가져오기 (브라우저 컨텍스트의 쿠키 자동 사용)
                String html = playwrightManager.fetchHtmlWithPlaywright(job.getDetailUrl(), GlobalConstants.LIST_PAGE_URL);

                // 파싱
                DetailInfoDTO detailInfo = htmlParserService.parseDetailPage(html, job.getIdx());

                // 업데이트
                job.updateDetailInfo(
                        detailInfo.getPhone(),
                        detailInfo.getCompanyAddress(),
                        detailInfo.getCompanyPhone(),
                        detailInfo.getRecruiterName(),
                        detailInfo.getRecruiterContact()
                );

                // 상세 정보 로그 출력
                log.info("  ✓ idx={} 완료", job.getIdx());
                log.info("    - 담당자: {}", detailInfo.getRecruiterName() != null ? detailInfo.getRecruiterName() : "없음");
                log.info("    - 담당자연락처: {}", detailInfo.getRecruiterContact() != null ? detailInfo.getRecruiterContact() : "없음");
                log.info("    - 회사주소: {}", detailInfo.getCompanyAddress() != null ? detailInfo.getCompanyAddress() : "없음");
                log.info("    - 회사연락처: {}", detailInfo.getCompanyPhone() != null ? detailInfo.getCompanyPhone() : "없음");
                log.info("    - 상세요강 전화번호: {}", detailInfo.getPhone() != null ? detailInfo.getPhone() : "없음");

                // 랜덤 딜레이
                if (i < jobs.size() - 1) {
                    int delay = scraperProperties.getRequestDelayMinMs()
                            + random.nextInt(scraperProperties.getRequestDelayMaxMs()
                            - scraperProperties.getRequestDelayMinMs());
                    log.info("  ⏱️  {}초 대기 중...", String.format("%.2f", delay / 1000.0));
                    Thread.sleep(delay);
                }

            } catch (Exception e) {
                log.warn("  ⚠ idx={} 오류: {}", job.getIdx(), e.getMessage());
                GlobalExceptionHandler.handleException(e, String.format("상세페이지 스크래핑 오류 (idx=%d, title=%s)", job.getIdx(), job.getTitle()));
            }
        }

        log.info("\n✓ 상세페이지 스크래핑 완료");
    }
}
