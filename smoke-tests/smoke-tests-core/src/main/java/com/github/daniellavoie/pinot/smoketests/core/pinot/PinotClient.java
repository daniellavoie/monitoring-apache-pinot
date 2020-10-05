package com.github.daniellavoie.pinot.smoketests.core.pinot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientResponseException.BadRequest;
import org.springframework.web.reactive.function.client.WebClientResponseException.InternalServerError;

import reactor.core.publisher.Mono;

public class PinotClient {
	private static final Logger LOGGER = LoggerFactory.getLogger(PinotClient.class);

	private final WebClient webClient;

	public PinotClient(String url) {
		this.webClient = WebClient.builder().baseUrl(url).build();
	}

	private void delete(String uri, Object... uriVariables) {
		webClient.delete().uri(uri, uriVariables).exchange().block();
	}

	public void deleteSchema(String schemaName) {
		delete("/schemas/{table}", schemaName);
	}

	public void deleteTable(String tableName) {
		delete("/tables/{table}", tableName);
	}

	public void createSchema(String schema) {
		postSync("/schemas", schema);
	}

	public void createTable(String table) {
		postSync("/tables", table);
	}

	private void postSync(String uri, Object body) {
		webClient.post().uri(uri).contentType(MediaType.APPLICATION_JSON).bodyValue(body).retrieve().toBodilessEntity()
				.doOnError(BadRequest.class,
						ex -> LOGGER.error(
								"Request body : " + body + "\n\nResponse body : " + ex.getResponseBodyAsString()))
				.doOnError(InternalServerError.class,
						ex -> LOGGER.error(
								"Request body : " + body + "\n\nResponse body : " + ex.getResponseBodyAsString()))
				.block();
	}

	public Mono<ResponseEntity<Void>> query(String sql) {
		return webClient.post().uri("/sql").contentType(MediaType.APPLICATION_JSON).bodyValue(new SqlRequest(sql))
				.retrieve().toBodilessEntity().doOnError(WebClientResponseException.class,
						ex -> LOGGER.error("Response body : " + ex.getResponseBodyAsString()));
	}

	public class SqlRequest {
		private final String sql;

		public SqlRequest(String sql) {
			this.sql = sql;
		}

		public String getSql() {
			return sql;
		}
	}
}
