package org.nicenomu.hairinjob.domain.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.common.exception.GlobalExceptionHandler;
import org.nicenomu.hairinjob.common.config.ScraperProperties;
import org.nicenomu.hairinjob.common.util.GlobalConstants;
import org.nicenomu.hairinjob.domain.job.model.dto.JobPostingDTO;
import org.nicenomu.hairinjob.domain.job.model.entity.JobPostingEntity;
import org.nicenomu.hairinjob.domain.job.model.entity.ScrapeSessionEntity;
import org.nicenomu.hairinjob.domain.job.repository.JobPostingRepository;
import org.nicenomu.hairinjob.infrastructure.browser.PlaywrightManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageScrapingService {

    private final PlaywrightManager playwrightManager;
    private final HtmlParserService htmlParserService;
    private final JobPostingRepository jobPostingRepository;
    private final ScraperProperties scraperProperties;
    private final Random random = new Random();

    @Async("scraperTaskExecutor")
    public CompletableFuture<List<JobPostingDTO>> scrapePagesAsync(
            int cookieIdx,
            int startPage,
            int endPage,
            ScrapeSessionEntity session
    ) {
        log.info("\n[쿠키 세트 {}/{}] 페이지 범위: {}~{} ({}페이지)",
                cookieIdx, GlobalConstants.DEFAULT_COOKIE_WORKERS,
                startPage, endPage, endPage - startPage + 1);

        List<JobPostingDTO> allJobs = new ArrayList<>();

        try {
            int totalPages = endPage - startPage + 1;

            for (int pageNum = startPage; pageNum <= endPage; pageNum++) {
                int currentIndex = pageNum - startPage + 1;
                double progress = (currentIndex * 100.0) / totalPages;

                log.info("  [{}] [{}/{}] 페이지 {} 스크래핑 중... ({}%)",
                        cookieIdx, currentIndex, totalPages, pageNum, String.format("%.1f", progress));

                // URL 및 Referer 구성
                String url;
                String referer;
                if (pageNum == 1) {
                    url = GlobalConstants.LIST_PAGE_URL;
                    referer = GlobalConstants.LIST_PAGE_URL;
                } else {
                    url = GlobalConstants.LIST_PAGE_URL + "?page=" + pageNum;
                    referer = GlobalConstants.LIST_PAGE_URL + "?page=" + (pageNum - 1);
                }

                // HTML 가져오기 (Playwright APIRequest 사용)
                String html = playwrightManager.fetchHtmlWithPlaywright(url, referer);

                // 파싱
                List<JobPostingDTO> jobs = htmlParserService.parseJobListings(html, pageNum, cookieIdx - 1);
                allJobs.addAll(jobs);

                log.info("  [{}] 페이지 {} 완료: {}개 항목", cookieIdx, pageNum, jobs.size());

                // 페이지 단위로 즉시 DB 저장 (Python과 동일)
                saveJobsImmediately(jobs, session);
                log.info("  [{}] 페이지 {} DB 저장 완료", cookieIdx, pageNum);

                // 랜덤 딜레이
                if (pageNum < endPage) {
                    int delay = scraperProperties.getRequestDelayMinMs()
                            + random.nextInt(scraperProperties.getRequestDelayMaxMs()
                            - scraperProperties.getRequestDelayMinMs());
                    Thread.sleep(delay);
                }
            }

            log.info("\n✓ 쿠키 세트 {} 완료: 총 {}개 항목 수집", cookieIdx, allJobs.size());
            return CompletableFuture.completedFuture(allJobs);

        } catch (Exception e) {
            log.error("쿠키 세트 {} 스크래핑 중 오류", cookieIdx, e);
            GlobalExceptionHandler.handleException(e, String.format("쿠키 세트 %d 페이지 스크래핑 중 오류 (페이지 범위: %d~%d)", cookieIdx, startPage, endPage));
            return CompletableFuture.failedFuture(e);
        }
    }

    public int detectTotalPages() {
        try {
            log.info("전체 페이지 수 감지 중...");
            String html = playwrightManager.fetchHtmlWithPlaywright(GlobalConstants.LIST_PAGE_URL, GlobalConstants.LIST_PAGE_URL);
            int totalPages = htmlParserService.getLastPageNumber(html);
            log.info("✓ 전체 페이지 수: {}", totalPages);
            return totalPages;
        } catch (Exception e) {
            log.error("페이지 수 감지 실패", e);
            GlobalExceptionHandler.handleException(e, "전체 페이지 수 감지 중 오류");
            throw new RuntimeException("페이지 수 감지 실패", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveJobsImmediately(List<JobPostingDTO> jobs, ScrapeSessionEntity session) {
        int saved = 0;
        for (JobPostingDTO dto : jobs) {
            try {
                JobPostingEntity entity = dto.toEntity(session);
                jobPostingRepository.save(entity);
                saved++;
            } catch (Exception e) {
                log.warn("  데이터 저장 실패: idx={}", dto.getIdx(), e);
            }
        }
    }
}
