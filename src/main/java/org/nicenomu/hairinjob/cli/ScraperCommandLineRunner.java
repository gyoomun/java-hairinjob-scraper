package org.nicenomu.hairinjob.cli;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.domain.job.service.JobScrapingOrchestratorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScraperCommandLineRunner implements CommandLineRunner {

    private final JobScrapingOrchestratorService orchestratorService;

    @Override
    public void run(String... args) throws Exception {
        if (args.length == 0) {
            printUsage();
            return;
        }

        String command = args[0];
        List<String> options = Arrays.asList(args).subList(1, args.length);

        switch (command) {
            case "collect-cookies":
                handleCollectCookies();
                break;
            case "test-page":
                handleTestPage();
                break;
            case "scrape-all":
                handleScrapeAll(options);
                break;
            case "test-detail":
                handleTestDetail();
                break;
            default:
                log.error("알 수 없는 명령어: {}", command);
                printUsage();
                System.exit(1);
        }
    }

    private void handleCollectCookies() {
        log.info("\n{'='*80}");
        log.info("[모드: 쿠키 수집] Playwright로 쿠키 수집 및 저장");
        log.info("{'='*80}");

        try {
            orchestratorService.collectCookies();
            System.exit(0);
        } catch (Exception e) {
            log.error("쿠키 수집 중 오류", e);
            System.exit(1);
        }
    }

    private void handleTestPage() {
        log.info("\n{'='*80}");
        log.info("[모드: 페이지 테스트] 저장된 쿠키로 1페이지 요청");
        log.info("{'='*80}");

        try {
            orchestratorService.testPage();
            System.exit(0);
        } catch (Exception e) {
            log.error("페이지 테스트 중 오류", e);
            System.exit(1);
        }
    }

    private void handleTestDetail() {
        log.info("\n{'='*80}");
        log.info("[모드: 상세 페이지 테스트] idx=101200 테스트");
        log.info("{'='*80}");

        try {
            orchestratorService.testDetail();
            System.exit(0);
        } catch (Exception e) {
            log.error("상세페이지 테스트 중 오류", e);
            System.exit(1);
        }
    }

    private void handleScrapeAll(List<String> options) {
        boolean skipPages = options.contains("--skip-pages");

        // storage 옵션 파싱
        String storage = "db"; // 기본값
        for (int i = 0; i < options.size(); i++) {
            if ("--storage".equals(options.get(i)) && i + 1 < options.size()) {
                storage = options.get(i + 1);
                if (!storage.equals("json") && !storage.equals("db") && !storage.equals("both")) {
                    log.error("잘못된 storage 옵션: {}. 'json', 'db', 'both' 중 하나를 선택하세요.", storage);
                    System.exit(1);
                }
                break;
            }
        }

        log.info("\n{'='*80}");
        log.info("명령어: scrape-all");
        log.info("옵션:");
        log.info("  --skip-pages: {}", skipPages);
        log.info("  --storage: {}", storage);
        log.info("{'='*80}");

        try {
            orchestratorService.scrapeAll(skipPages, storage);
            System.exit(0);
        } catch (Exception e) {
            log.error("스크래핑 실행 중 오류", e);
            System.exit(1);
        }
    }

    private void printUsage() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("HairinJob 스크래퍼 - 사용법");
        System.out.println("=".repeat(80));
        System.out.println("\n  # 쿠키 수집");
        System.out.println("  ./gradlew bootRun --args='collect-cookies'");
        System.out.println("  java -jar java-hairinjob.jar collect-cookies");
        System.out.println("\n  # 페이지 테스트 (1페이지)");
        System.out.println("  ./gradlew bootRun --args='test-page'");
        System.out.println("  java -jar java-hairinjob.jar test-page");
        System.out.println("\n  # 상세페이지 테스트");
        System.out.println("  ./gradlew bootRun --args='test-detail'");
        System.out.println("  java -jar java-hairinjob.jar test-detail");
        System.out.println("\n  # 전체 스크래핑");
        System.out.println("  # 저장매체 -> storage=[db, json, both]");
        System.out.println("  # 목록 스크랩없이 저장된 detail url 로 상세페이지 스크랩 -> skip-pages");
        System.out.println("  ./gradlew bootRun --args='scrape-all'");
        System.out.println("  ./gradlew bootRun --args='scrape-all --storage db'");
        System.out.println("  ./gradlew bootRun --args='scrape-all --storage json'");
        System.out.println("  ./gradlew bootRun --args='scrape-all --storage both'");
        System.out.println("  ./gradlew bootRun --args='scrape-all --skip-pages'");
        System.out.println("  java -jar java-hairinjob.jar scrape-all");
        System.out.println("  java -jar java-hairinjob.jar scrape-all --storage db");
        System.out.println("  java -jar java-hairinjob.jar scrape-all --storage json");
        System.out.println("  java -jar java-hairinjob.jar scrape-all --storage both");
        System.out.println("  java -jar java-hairinjob.jar scrape-all --skip-pages");
        System.out.println("\n" + "=".repeat(80));
    }
}
