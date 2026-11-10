package com.example.codeplaxpagamentosapi.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class ClienteHttpConfig {
  @Bean
  public RestClient bilheteriaClient(@Value("${bilheteria.url}") String url) {
    var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    var factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(3));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }
}
