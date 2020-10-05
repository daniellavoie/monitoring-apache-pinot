package com.github.daniellavoie.pinot.smoketests.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.daniellavoie.pinot.smoketests.core.pinot.PinotClient;


@Configuration
public class PinotConfiguration {
  @Bean
  public PinotClient pinotClient(@Value("${pinot.url:http://localhost:9000}") String pinotUrl) {
    return new PinotClient(pinotUrl);
  }
}
