package org.nicenomu.hairinjob.domain.job.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.common.util.GlobalConstants;
import org.nicenomu.hairinjob.domain.job.model.dto.JobPostingDTO;
import org.nicenomu.hairinjob.domain.job.model.entity.JobPostingEntity;
import org.nicenomu.hairinjob.domain.job.model.entity.ScrapeSessionEntity;
import org.nicenomu.hairinjob.domain.job.repository.JobPostingRepository;
import org.nicenomu.hairinjob.domain.job.repository.ScrapeSessionRepository;
import org.nicenomu.hairinjob.infrastructure.browser.PlaywrightManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobScrapingOrchestratorService {

    private final PlaywrightManager playwrightManager;
    private final PageScrapingService pageScrapingService;
    private final DetailScrapingService detailScrapingService;
    private final ScrapeSessionRepository sessionRepository;
    private final JobPostingRepository jobPostingRepository;

    @Transactional
    public void scrapeAll(boolean skipPages) {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("\n{'='*80}");
        log.info("HairinJob 스크래핑 시작");
        log.info("시작 시간: {}", startTime);
        log.info("{'='*80}");

        try {
            // 1. Playwright 브라우저 초기화 및 쿠키 추출
            log.info("\n[1단계] 브라우저 초기화 및 로그인...");
            Map<String, String> cookies = playwrightManager.initializeAndGetCookies();

            ScrapeSessionEntity session = null;
            List<JobPostingDTO> allJobs = new ArrayList<>();

            if (!skipPages) {
                // 2. 세션 시작
                log.info("\n[2단계] 스크래핑 세션 생성...");
                session = createSession();

                // 3. 전체 페이지 수 감지
                log.info("\n[3단계] 전체 페이지 수 감지...");
                int totalPages = pageScrapingService.detectTotalPages(cookies);

                // 4. 페이지 범위 분할 (10개 워커)
                List<int[]> ranges = dividePages(totalPages, GlobalConstants.DEFAULT_COOKIE_WORKERS);

                // 5. 병렬 스크래핑
                log.info("\n[4단계] 병렬 페이지 스크래핑 시작...");
                allJobs = scrapeParallel(cookies, ranges, session);

                // 6. 데이터베이스 저장
                log.info("\n[5단계] 데이터베이스 저장 중...");
                saveJobs(allJobs, session);

                // 7. 세션 완료
                session.complete(allJobs.size(), allJobs.size(), 0);
                sessionRepository.save(session);
            }

            // 8. 상세페이지 스크래핑
            log.info("\n[6단계] 상세페이지 스크래핑...");
            detailScrapingService.scrapeAllDetailPages(cookies);

            // 9. 종료
            LocalDateTime endTime = LocalDateTime.now();
            Duration duration = Duration.between(startTime, endTime);
            long minutes = duration.toMinutes();
            long seconds = duration.minusMinutes(minutes).getSeconds();

            log.info("\n{'='*80}");
            log.info("✓ 스크래핑 완료!");
            log.info("총 수집: {}개", allJobs.size());
            log.info("소요 시간: {}분 {}초", minutes, seconds);
            log.info("{'='*80}");

        } catch (Exception e) {
            log.error("스크래핑 중 오류 발생", e);
            throw new RuntimeException("스크래핑 실패", e);
        } finally {
            playwrightManager.closeBrowser();
        }
    }

    private ScrapeSessionEntity createSession() {
        ScrapeSessionEntity session = ScrapeSessionEntity.builder()
                .startedAt(LocalDateTime.now())
                .numCookies(GlobalConstants.DEFAULT_COOKIE_WORKERS)
                .status("RUNNING")
                .build();
        return sessionRepository.save(session);
    }

    private List<int[]> dividePages(int totalPages, int numWorkers) {
        List<int[]> ranges = new ArrayList<>();
        int pagesPerWorker = (int) Math.ceil((double) totalPages / numWorkers);

        for (int i = 0; i < numWorkers; i++) {
            int startPage = i * pagesPerWorker + 1;
            int endPage = Math.min((i + 1) * pagesPerWorker, totalPages);

            if (startPage <= totalPages) {
                ranges.add(new int[]{startPage, endPage});
            }
        }

        return ranges;
    }

    private List<JobPostingDTO> scrapeParallel(
            Map<String, String> cookies,
            List<int[]> ranges,
            ScrapeSessionEntity session
    ) {
        List<CompletableFuture<List<JobPostingDTO>>> futures = new ArrayList<>();

        for (int i = 0; i < ranges.size(); i++) {
            int[] range = ranges.get(i);
            int cookieIdx = i + 1;
            CompletableFuture<List<JobPostingDTO>> future = pageScrapingService.scrapePagesAsync(
                    cookieIdx, cookies, range[0], range[1], session
            );
            futures.add(future);
        }

        // 모든 작업 완료 대기
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        try {
            allOf.join();
        } catch (Exception e) {
            log.error("병렬 스크래핑 중 오류", e);
        }

        // 결과 수집
        List<JobPostingDTO> allJobs = new ArrayList<>();
        for (CompletableFuture<List<JobPostingDTO>> future : futures) {
            try {
                allJobs.addAll(future.get());
            } catch (Exception e) {
                log.error("작업 결과 수집 중 오류", e);
            }
        }

        return allJobs;
    }

    @Transactional
    protected void saveJobs(List<JobPostingDTO> jobs, ScrapeSessionEntity session) {
        int saved = 0;
        for (JobPostingDTO dto : jobs) {
            try {
                JobPostingEntity entity = dto.toEntity(session);
                jobPostingRepository.save(entity);
                saved++;
            } catch (Exception e) {
                log.warn("데이터 저장 실패: idx={}", dto.getIdx(), e);
            }
        }
        log.info("✓ {}개 / {}개 저장 완료", saved, jobs.size());
    }
}
