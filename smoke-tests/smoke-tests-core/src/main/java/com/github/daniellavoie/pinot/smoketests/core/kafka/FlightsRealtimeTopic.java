package com.github.daniellavoie.pinot.smoketests.core.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("pinot.topics.flights-realtime")
public class FlightsRealtimeTopic extends TopicConfiguration {

}
