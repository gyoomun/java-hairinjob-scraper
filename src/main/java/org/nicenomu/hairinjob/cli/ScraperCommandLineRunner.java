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
            case "scrape-all":
                handleScrapeAll(options);
                break;
            case "help":
            case "--help":
            case "-h":
                printUsage();
                break;
            default:
                log.error("알 수 없는 명령어: {}", command);
                printUsage();
                System.exit(1);
        }
    }

    private void handleScrapeAll(List<String> options) {
        boolean skipPages = options.contains("--skip-pages");

        log.info("\n{'='*80}");
        log.info("명령어: scrape-all");
        log.info("옵션:");
        log.info("  --skip-pages: {}", skipPages);
        log.info("{'='*80}");

        try {
            orchestratorService.scrapeAll(skipPages);
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
        System.out.println("\n사용법:");
        System.out.println("  java -jar java-hairinjob.jar <command> [options]");
        System.out.println("\n명령어:");
        System.out.println("  scrape-all              전체 스크래핑 실행 (페이지 + 상세)");
        System.out.println("  help, --help, -h        도움말 출력");
        System.out.println("\n옵션:");
        System.out.println("  --skip-pages            페이지 스크래핑 건너뛰고 상세페이지만 실행");
        System.out.println("\n예시:");
        System.out.println("  # 전체 스크래핑");
        System.out.println("  java -jar java-hairinjob.jar scrape-all");
        System.out.println("\n  # 상세페이지만 스크래핑");
        System.out.println("  java -jar java-hairinjob.jar scrape-all --skip-pages");
        System.out.println("\n환경 변수:");
        System.out.println("  DB_URL                  데이터베이스 URL");
        System.out.println("  DB_USER                 데이터베이스 사용자");
        System.out.println("  DB_PASSWORD             데이터베이스 비밀번호");
        System.out.println("  SCRAPER_USERNAME        로그인 아이디");
        System.out.println("  SCRAPER_PASSWORD        로그인 비밀번호");
        System.out.println("  USER_DATA_DIR           브라우저 데이터 디렉토리");
        System.out.println("  HEADLESS                헤드리스 모드 (true/false)");
        System.out.println("\n" + "=".repeat(80));
    }
}
