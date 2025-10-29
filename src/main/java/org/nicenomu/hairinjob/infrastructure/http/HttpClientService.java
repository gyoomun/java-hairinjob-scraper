package org.nicenomu.hairinjob.infrastructure.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpClientService {

    private final RestClient restClient;

    public String get(String url, Map<String, String> cookies) {
        try {
            String cookieHeader = buildCookieHeader(cookies);

            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, cookieHeader)
                    .header(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .retrieve()
                    .body(String.class);

        } catch (Exception e) {
            log.error("HTTP GET 요청 실패: {}", url, e);
            throw new RuntimeException("HTTP 요청 실패", e);
        }
    }

    private String buildCookieHeader(Map<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        cookies.forEach((key, value) -> {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(key).append("=").append(value);
        });
        return sb.toString();
    }
}
