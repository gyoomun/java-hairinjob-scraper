package org.nicenomu.hairinjob.common.util;

public class GlobalConstants {

    private GlobalConstants() {
        throw new IllegalStateException("Utility class");
    }

    // 기본 URL
    public static final String BASE_URL = "https://www.hairinjob.com";
    public static final String LIST_PAGE_URL = BASE_URL + "/cms/s01.php";
    public static final String DETAIL_PAGE_URL = BASE_URL + "/cms/s01_v.php";
    public static final String LOGIN_URL = BASE_URL + "/cms/user/login.php";
    public static final String MYPAGE_URL = BASE_URL + "/cms/p/mypage.php";

    // 스크래핑 설정
    public static final int DEFAULT_COOKIE_WORKERS = 10;
    public static final int COOKIE_REFRESH_INTERVAL_MINUTES = 20;
    public static final int REQUEST_DELAY_MIN_MS = 300;
    public static final int REQUEST_DELAY_MAX_MS = 1000;

    // Playwright 설정
    public static final int BROWSER_TIMEOUT_MS = 30000;
    public static final int PAGE_LOAD_WAIT_MS = 4000;
    public static final int POST_LOGIN_WAIT_MS = 3000;
    public static final int AUTO_CLEANUP_DAYS = 7;

    // 셀렉터
    public static final String LOGIN_USERNAME_SELECTOR = "input[name='userid']";
    public static final String LOGIN_PASSWORD_SELECTOR = "input[name='pwd']";
    public static final String LOGIN_BUTTON_SELECTOR = "button.btn_login";
}
