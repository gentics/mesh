package com.gentics.mesh.test.docker;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	public static final String VERSION = "6.1.2";

	public ElasticsearchContainer() {
		super("docker.elastic.co/elasticsearch/elasticsearch:" + VERSION);
	}

	@Override
	protected void configure() {
		addEnv("discovery.type", "single-node");
		addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofSeconds(250L));
		waitingFor(Wait.forHttp("/"));
	}

}
