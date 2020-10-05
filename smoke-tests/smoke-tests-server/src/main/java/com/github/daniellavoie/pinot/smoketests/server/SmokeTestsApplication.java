package com.github.daniellavoie.pinot.smoketests.server;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import com.github.daniellavoie.pinot.smoketests.core.RealtimeServiceImpl;


@Configuration
@SpringBootApplication
public class SmokeTestsApplication implements CommandLineRunner {
  private final RealtimeServiceImpl realtimeService;

  public static void main(String[] args) {
    SpringApplication.run(SmokeTestsApplication.class, args);
  }

  public SmokeTestsApplication(RealtimeServiceImpl realtimeService) {
    this.realtimeService = realtimeService;
  }

  @Override
  public void run(String... args) throws Exception {
    realtimeService.produceData();
  }
}
