package com.gentics.mesh.test.docker;

import java.time.Duration;
import java.util.Collections;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	public static final String VERSION = "6.6.1";

	public ElasticsearchContainer(boolean withIngestPlugin) {
		super(withIngestPlugin ? "jotschi/elasticsearch-ingest:" + VERSION : "docker.elastic.co/elasticsearch/elasticsearch-oss:" + VERSION);
	}

	@Override
	protected void configure() {
		addEnv("discovery.type", "single-node");
		withTmpFs(Collections.singletonMap("/usr/share/elasticsearch/data", "rw,size=64m"));
		// addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofSeconds(250L));
		waitingFor(new HttpWaitStrategy().forPath("/"));
	}

}
