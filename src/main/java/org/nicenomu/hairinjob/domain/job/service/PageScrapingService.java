package org.nicenomu.hairinjob.domain.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.common.config.ScraperProperties;
import org.nicenomu.hairinjob.common.util.GlobalConstants;
import org.nicenomu.hairinjob.domain.job.model.dto.JobPostingDTO;
import org.nicenomu.hairinjob.domain.job.model.entity.ScrapeSessionEntity;
import org.nicenomu.hairinjob.infrastructure.http.HttpClientService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class PageScrapingService {

    private final HttpClientService httpClientService;
    private final HtmlParserService htmlParserService;
    private final ScraperProperties scraperProperties;
    private final Random random = new Random();

    @Async("scraperTaskExecutor")
    public CompletableFuture<List<JobPostingDTO>> scrapePagesAsync(
            int cookieIdx,
            Map<String, String> cookies,
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

                log.info("  [{}] [{}/{}] 페이지 {} 스크래핑 중... ({:.1f}%)",
                        cookieIdx, currentIndex, totalPages, pageNum, progress);

                // URL 구성
                String url = (pageNum == 1)
                        ? GlobalConstants.LIST_PAGE_URL
                        : GlobalConstants.LIST_PAGE_URL + "?page=" + pageNum;

                // HTML 가져오기
                String html = httpClientService.get(url, cookies);

                // 파싱
                List<JobPostingDTO> jobs = htmlParserService.parseJobListings(html, pageNum, cookieIdx - 1);
                allJobs.addAll(jobs);

                log.info("  [{}] 페이지 {} 완료: {}개 항목", cookieIdx, pageNum, jobs.size());

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
            return CompletableFuture.failedFuture(e);
        }
    }

    public int detectTotalPages(Map<String, String> cookies) {
        try {
            log.info("전체 페이지 수 감지 중...");
            String html = httpClientService.get(GlobalConstants.LIST_PAGE_URL, cookies);
            int totalPages = htmlParserService.getLastPageNumber(html);
            log.info("✓ 전체 페이지 수: {}", totalPages);
            return totalPages;
        } catch (Exception e) {
            log.error("페이지 수 감지 실패", e);
            throw new RuntimeException("페이지 수 감지 실패", e);
        }
    }
}
