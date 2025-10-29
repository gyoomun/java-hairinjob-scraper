package org.nicenomu.hairinjob.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "scraper")
@Getter
@Setter
public class ScraperProperties {

    private String targetUrl;
    private Login login = new Login();
    private Browser browser = new Browser();
    private Integer cookieRefreshIntervalMinutes;
    private Integer requestDelayMinMs;
    private Integer requestDelayMaxMs;

    @Getter
    @Setter
    public static class Login {
        private String username;
        private String password;
    }

    @Getter
    @Setter
    public static class Browser {
        private String userDataDir;
        private Boolean headless;
    }
}
