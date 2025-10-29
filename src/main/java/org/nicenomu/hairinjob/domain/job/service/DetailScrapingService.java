package org.nicenomu.hairinjob.domain.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.common.config.ScraperProperties;
import org.nicenomu.hairinjob.domain.job.model.dto.DetailInfoDTO;
import org.nicenomu.hairinjob.domain.job.model.entity.JobPostingEntity;
import org.nicenomu.hairinjob.domain.job.repository.JobPostingRepository;
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
    private final HttpClientService httpClientService;
    private final HtmlParserService htmlParserService;
    private final ScraperProperties scraperProperties;
    private final Random random = new Random();

    @Transactional
    public void scrapeAllDetailPages(Map<String, String> cookies) {
        List<JobPostingEntity> jobs = jobPostingRepository.findAllDetailUrlsFromLatestSession();

        log.info("\n상세페이지 스크래핑 시작: 총 {}개", jobs.size());

        Instant lastCookieRefresh = Instant.now();
        Duration cookieRefreshInterval = Duration.ofMinutes(
                scraperProperties.getCookieRefreshIntervalMinutes()
        );

        for (int i = 0; i < jobs.size(); i++) {
            JobPostingEntity job = jobs.get(i);

            log.info("[{}/{}] idx={} 스크래핑 중...", i + 1, jobs.size(), job.getIdx());

            try {
                // 쿠키 갱신 체크 (20분마다)
                if (Duration.between(lastCookieRefresh, Instant.now()).compareTo(cookieRefreshInterval) > 0) {
                    log.info("\n🔄 쿠키 갱신 필요 (20분 경과)");
                    // TODO: PlaywrightManager에서 쿠키 재갱신
                    lastCookieRefresh = Instant.now();
                }

                // HTML 가져오기
                String html = httpClientService.get(job.getDetailUrl(), cookies);

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

                log.info("  ✓ idx={} 완료", job.getIdx());

                // 랜덤 딜레이
                if (i < jobs.size() - 1) {
                    int delay = scraperProperties.getRequestDelayMinMs()
                            + random.nextInt(scraperProperties.getRequestDelayMaxMs()
                            - scraperProperties.getRequestDelayMinMs());
                    log.info("  ⏱️  {:.2f}초 대기 중...", delay / 1000.0);
                    Thread.sleep(delay);
                }

            } catch (Exception e) {
                log.warn("  ⚠ idx={} 오류: {}", job.getIdx(), e.getMessage());
            }
        }

        log.info("\n✓ 상세페이지 스크래핑 완료");
    }
}
