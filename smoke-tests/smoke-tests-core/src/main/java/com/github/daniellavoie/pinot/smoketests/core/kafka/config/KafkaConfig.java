package com.github.daniellavoie.pinot.smoketests.core.kafka.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;


@Configuration
public class KafkaConfig {
  @Bean
  public KafkaTemplate<String, String> exchangeRateTemplate(KafkaProperties kafkaProperties) {
    return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties()));
  }
}
