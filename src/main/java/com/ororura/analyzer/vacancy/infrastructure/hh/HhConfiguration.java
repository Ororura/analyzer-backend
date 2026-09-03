package com.ororura.analyzer.vacancy.infrastructure.hh;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HhProperties.class)
class HhConfiguration {

    static final String REST_CLIENT = "hhRestClient";

    @Bean
    @Qualifier(REST_CLIENT)
    RestClient hhRestClient(HhProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader("Accept", "text/html,application/xhtml+xml")
                .defaultHeader("Accept-Language", "ru-RU,ru;q=0.9")
                .defaultHeader("User-Agent", "pdf-analyzer/1.0 (public vacancy reader)")
                .build();
    }
}
