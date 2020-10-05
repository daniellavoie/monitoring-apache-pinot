package com.github.daniellavoie.pinot.smoketests.core;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.github.daniellavoie.pinot.smoketests.core.config.TestDataConfiguration;
import com.github.daniellavoie.pinot.smoketests.core.kafka.FlightsRealtimeTopic;
import com.github.daniellavoie.pinot.smoketests.core.kafka.TopicConfiguration;
import com.github.daniellavoie.pinot.smoketests.core.pinot.PinotClient;

import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Service
public class RealtimeServiceImpl {
	private final Logger LOGGER = LoggerFactory.getLogger(RealtimeServiceImpl.class);

	private final int messageDelayMillis;
	private final String kafkaBrokerList;
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final FlightsRealtimeTopic flightsRealtimeTopic;
	private final PinotClient pinotClient;
	private final TestDataConfiguration testDataConfiguration;
	private final AdminClient adminClient;

	private boolean shutingDown;

	public RealtimeServiceImpl(@Value("${pinot.data.delay-millis:1000}") int messageDelayMillis,
			@Value("${spring.kafka.bootstrap-servers:localhost:9092}") String kafkaBrokerList,
			KafkaTemplate<String, String> kafkaTemplate, FlightsRealtimeTopic flightsRealtimeTopic,
			PinotClient pinotClient, TestDataConfiguration testDataConfiguration, KafkaProperties kafkaProperties) {
		this.messageDelayMillis = messageDelayMillis;
		this.kafkaBrokerList = kafkaBrokerList;
		this.kafkaTemplate = kafkaTemplate;
		this.flightsRealtimeTopic = flightsRealtimeTopic;
		this.pinotClient = pinotClient;
		this.testDataConfiguration = testDataConfiguration;

		this.adminClient = AdminClient.create(kafkaProperties.buildAdminProperties());
	}

	public void produceData() {
		LOGGER.info("Producing data to kafka brokers : {}", kafkaBrokerList);

		createTopicIfMissing(flightsRealtimeTopic, adminClient);

		pinotClient.deleteTable("pinotSmokeTests");
		pinotClient.deleteSchema("pinotSmokeTests");

		pinotClient.createSchema(testDataConfiguration.getSchema());
		pinotClient.createTable(testDataConfiguration.getTable().replace("${kafka.broker-list}", kafkaBrokerList));

		Flux.range(0, Integer.MAX_VALUE).doOnNext(index -> {
			LocalDateTime nextEmission = LocalDateTime.now();
			String line = null;
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new ClassPathResource("data/airlineStats_data.json").getInputStream()))) {
				do {
					line = reader.readLine();
					if (LocalDateTime.now().isAfter(nextEmission)) {
						kafkaTemplate.send(flightsRealtimeTopic.getName(), line).get();

						LOGGER.info("Message sent to kafka client.");

						nextEmission = nextEmission.plus(Duration.ofMillis(messageDelayMillis));
					} else {
						Thread.sleep(100);
					}
				} while (!shutingDown && line != null);

				if (shutingDown) {
					throw new RuntimeException("Shutting down.");
				}
			} catch (Exception e) {
				LOGGER.error("Shutting down", e);

				System.exit(1);
			}
		}).subscribeOn(Schedulers.newSingle("smoke-tests-publisher")).subscribe();

		pinotClient.query("select * from pinotSmokeTests limit 10")

				.repeat()

				.buffer(Duration.ofSeconds(1))

				.doOnNext(responses -> LOGGER.info("Query QPS : {}", responses.size()))

				.subscribeOn(Schedulers.elastic())

				.publishOn(Schedulers.elastic()).subscribe();
	}

	public void close() {
		shutingDown = true;
	}

	private void createTopicIfMissing(TopicConfiguration topicConfiguration, AdminClient adminClient) {
		try {
			createTopicIfMissing(topicConfiguration, adminClient, topicConfiguration.getReplicationFactor());
		} catch (RuntimeException ex) {
			if (topicConfiguration.getReplicationFactor() == 1) {
				createTopicIfMissing(topicConfiguration, adminClient, (short) 3);
			} else {
				throw ex;
			}
		}
	}

	private void createTopicIfMissing(TopicConfiguration topicConfiguration, AdminClient adminClient,
			short replicationFactor) {
		try {
			if (!adminClient.listTopics().names().get().stream()
					.filter(existingTopic -> existingTopic.equals(topicConfiguration.getName())).findAny()
					.isPresent()) {
				LOGGER.info("Creating topic {}.", topicConfiguration.getName());

				NewTopic topic = new NewTopic(topicConfiguration.getName(), topicConfiguration.getPartitions(),
						replicationFactor);

				topic.configs(new HashMap<>());
				if (topicConfiguration.isCompacted()) {
					topic.configs().put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
				}
				adminClient.createTopics(Arrays.asList(topic)).all().get();
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}
}
