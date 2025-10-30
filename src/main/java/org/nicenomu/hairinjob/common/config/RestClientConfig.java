package org.nicenomu.hairinjob.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder
                .requestFactory(clientHttpRequestFactory())
                .build();
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        // Python과 동일하게 무제한 타임아웃 (Duration.ZERO = 무제한)
        factory.setReadTimeout(Duration.ZERO);
        return factory;
    }
}
