package org.nicenomu.hairinjob.infrastructure.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HttpClientService {

    private final RestClient restClient;

    public String get(String url, Map<String, String> cookies) {
        return get(url, cookies, null);
    }

    public String get(String url, Map<String, String> cookies, String referer) {
        try {
            String cookieHeader = buildCookieHeader(cookies);

            byte[] responseBytes = restClient.get()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, cookieHeader)
                    .header(HttpHeaders.USER_AGENT,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header(HttpHeaders.ACCEPT,
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header(HttpHeaders.ACCEPT_LANGUAGE,
                            "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header(HttpHeaders.ACCEPT_ENCODING,
                            "gzip, deflate, br")
                    .header(HttpHeaders.CONNECTION,
                            "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .headers(headers -> {
                        if (referer != null && !referer.isEmpty()) {
                            headers.set(HttpHeaders.REFERER, referer);
                        }
                    })
                    .retrieve()
                    .body(byte[].class);

            // euc-kr 인코딩으로 변환
            if (responseBytes != null) {
                return new String(responseBytes, Charset.forName("euc-kr"));
            }
            return "";

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
