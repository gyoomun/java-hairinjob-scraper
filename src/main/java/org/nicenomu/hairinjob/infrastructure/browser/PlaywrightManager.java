package org.nicenomu.hairinjob.infrastructure.browser;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nicenomu.hairinjob.common.config.ScraperProperties;
import org.nicenomu.hairinjob.common.util.GlobalConstants;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaywrightManager {

    private final ScraperProperties scraperProperties;
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;

    public Map<String, String> initializeAndGetCookies() {
        try {
            log.info("Playwright 브라우저 초기화 시작...");
            checkAndCleanupUserData();

            playwright = Playwright.create();
            browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions()
                            .setHeadless(scraperProperties.getBrowser().getHeadless())
            );

            Path userDataDir = Paths.get(scraperProperties.getBrowser().getUserDataDir());
            Path storageStatePath = userDataDir.resolve("storage-state.json");

            Browser.NewContextOptions contextOptions = new Browser.NewContextOptions();
            if (Files.exists(storageStatePath)) {
                contextOptions.setStorageStatePath(storageStatePath);
            }
            context = browser.newContext(contextOptions);

            Page page = context.newPage();
            page.navigate(GlobalConstants.BASE_URL);
            page.waitForLoadState();
            Thread.sleep(GlobalConstants.PAGE_LOAD_WAIT_MS);

            // 로그인 상태 확인 (페이지 HTML에서 URL 존재 여부로 판단)
            String pageContent = page.content();
            boolean isLoggedIn;

            if (pageContent.contains(GlobalConstants.LOGOUT_URL)) {
                isLoggedIn = true;
                log.info("✅ 로그인 상태: 로그인됨");
            } else if (pageContent.contains(GlobalConstants.LOGIN_URL)) {
                isLoggedIn = false;
                log.info("❌ 로그인 상태: 로그인 안됨");
            } else {
                isLoggedIn = false;
                log.warn("⚠️ 로그인 상태: 확인 불가 (페이지 구조 변경 가능성)");
            }

            if (!isLoggedIn) {
                log.info("로그인이 필요합니다. 로그인 진행...");
                performLogin(page);

                // 로그인 후 storage state 저장
                context.storageState(new BrowserContext.StorageStateOptions()
                        .setPath(storageStatePath));
                log.info("✓ Storage state 저장 완료: {}", storageStatePath);
            } else {
                log.info("이미 로그인되어 있습니다.");
            }

            // 쿠키 추출
            Map<String, String> cookies = extractCookies();
            log.info("✓ 쿠키 추출 완료 (총 {}개)", cookies.size());

            return cookies;

        } catch (Exception e) {
            log.error("Playwright 초기화 중 오류 발생", e);
            closeBrowser();
            throw new RuntimeException("브라우저 초기화 실패", e);
        }
    }

    private void performLogin(Page page) throws InterruptedException {
        page.navigate(GlobalConstants.LOGIN_URL);
        page.waitForLoadState();

        page.locator(GlobalConstants.LOGIN_USERNAME_SELECTOR).first().fill(
                scraperProperties.getLogin().getUsername()
        );
        page.locator(GlobalConstants.LOGIN_PASSWORD_SELECTOR).first().fill(
                scraperProperties.getLogin().getPassword()
        );

        Thread.sleep(GlobalConstants.POST_LOGIN_WAIT_MS);

        page.locator(GlobalConstants.LOGIN_BUTTON_SELECTOR).first().click();
        page.waitForURL(GlobalConstants.MYPAGE_URL, new Page.WaitForURLOptions()
                .setTimeout(GlobalConstants.BROWSER_TIMEOUT_MS));

        log.info("✓ 로그인 완료");
    }

    private Map<String, String> extractCookies() {
        Map<String, String> cookieMap = new HashMap<>();
        for (Cookie cookie : context.cookies()) {
            cookieMap.put(cookie.name, cookie.value);
        }
        return cookieMap;
    }

    public Map<String, String> refreshCookies() {
        log.info("🔄 쿠키 갱신 중...");
        if (context == null) {
            throw new IllegalStateException("BrowserContext가 초기화되지 않았습니다.");
        }

        try {
            // 페이지 새로고침
            Page page = context.pages().get(0);
            page.navigate(GlobalConstants.BASE_URL);
            page.waitForLoadState();
            Thread.sleep(GlobalConstants.PAGE_LOAD_WAIT_MS);

            // 로그인 상태 확인 (페이지 HTML에서 URL 존재 여부로 판단)
            String pageContent = page.content();
            boolean isLoggedIn;

            if (pageContent.contains(GlobalConstants.LOGOUT_URL)) {
                isLoggedIn = true;
                log.info("✅ 로그인 상태: 로그인됨");
            } else if (pageContent.contains(GlobalConstants.LOGIN_URL)) {
                isLoggedIn = false;
                log.info("❌ 로그인 상태: 로그인 안됨");
            } else {
                isLoggedIn = false;
                log.warn("⚠️ 로그인 상태: 확인 불가 (페이지 구조 변경 가능성)");
            }

            // 로그인 안되어 있으면 재로그인
            if (!isLoggedIn) {
                log.info("로그인이 필요합니다. 재로그인 진행...");
                performLogin(page);

                // 로그인 후 storage state 저장
                Path userDataDir = Paths.get(scraperProperties.getBrowser().getUserDataDir());
                Path storageStatePath = userDataDir.resolve("storage-state.json");
                context.storageState(new BrowserContext.StorageStateOptions()
                        .setPath(storageStatePath));
                log.info("✓ Storage state 갱신 완료: {}", storageStatePath);
            }

            // 새 쿠키 추출
            Map<String, String> cookies = extractCookies();
            log.info("✓ 쿠키 갱신 완료 (총 {}개)", cookies.size());
            return cookies;

        } catch (Exception e) {
            log.error("쿠키 갱신 중 오류 발생", e);
            throw new RuntimeException("쿠키 갱신 실패", e);
        }
    }

    private void checkAndCleanupUserData() {
        try {
            Path userDataDir = Paths.get(scraperProperties.getBrowser().getUserDataDir());
            if (!Files.exists(userDataDir)) {
                Files.createDirectories(userDataDir);
                return;
            }

            Path storageState = userDataDir.resolve("storage-state.json");
            if (Files.exists(storageState)) {
                Instant lastModified = Files.getLastModifiedTime(storageState).toInstant();
                Duration age = Duration.between(lastModified, Instant.now());

                if (age.toDays() >= GlobalConstants.AUTO_CLEANUP_DAYS) {
                    log.info("브라우저 데이터가 {}일 이상 경과하여 초기화합니다.", GlobalConstants.AUTO_CLEANUP_DAYS);
                    deleteDirectory(userDataDir);
                    Files.createDirectories(userDataDir);
                }
            }
        } catch (IOException e) {
            log.warn("브라우저 데이터 정리 중 오류 발생", e);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            Files.walk(path)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            log.warn("파일 삭제 실패: {}", p, e);
                        }
                    });
        }
    }

    public void closeBrowser() {
        if (context != null) {
            context.close();
            context = null;
        }
        if (browser != null) {
            browser.close();
            browser = null;
        }
        if (playwright != null) {
            playwright.close();
            playwright = null;
        }
        log.info("✓ 브라우저 종료 완료");
    }

    /**
     * Playwright의 APIRequest를 사용하여 HTTP GET 요청
     * 브라우저 컨텍스트의 쿠키와 세션이 자동으로 포함됨
     */
    public String fetchHtmlWithPlaywright(String url, String referer) {
        if (context == null) {
            throw new IllegalStateException("BrowserContext가 초기화되지 않았습니다.");
        }

        try {
            APIRequestContext apiRequestContext = playwright.request().newContext(
                    new APIRequest.NewContextOptions()
                            .setBaseURL(GlobalConstants.BASE_URL)
                            .setExtraHTTPHeaders(Map.of(
                                    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                                    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                                    "Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
                                    "Accept-Encoding", "gzip, deflate, br",
                                    "Connection", "keep-alive",
                                    "Upgrade-Insecure-Requests", "1",
                                    "Referer", referer != null ? referer : GlobalConstants.BASE_URL
                            ))
                            .setStorageState(context.storageState())  // 브라우저 컨텍스트의 쿠키 사용
            );

            APIResponse response = apiRequestContext.get(url);

            if (!response.ok()) {
                throw new RuntimeException("HTTP 요청 실패: " + response.status() + " " + response.statusText());
            }

            // euc-kr 인코딩으로 변환
            byte[] bodyBytes = response.body();
            String html = new String(bodyBytes, "euc-kr");

            apiRequestContext.dispose();
            return html;

        } catch (Exception e) {
            log.error("Playwright HTTP 요청 실패: {}", url, e);
            throw new RuntimeException("Playwright HTTP 요청 실패", e);
        }
    }

    public BrowserContext getContext() {
        return context;
    }

    public boolean isBrowserOpen() {
        return browser != null && browser.isConnected();
    }
}
