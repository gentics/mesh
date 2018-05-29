package com.gentics.mesh.test.docker;

import java.nio.charset.Charset;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.LazyFuture;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	public static final String VERSION = "6.1.2";
	private boolean withIngest = false;

	public ElasticsearchContainer(boolean withIngest) {
		super(prepareDockerImage(withIngest));
		this.withIngest = withIngest;
	}

	private static LazyFuture<String> prepareDockerImage(boolean withIngest) {
		if (withIngest) {
			try {
				ImageFromDockerfile dockerImage = new ImageFromDockerfile("elasticsearch", false);
				String dockerFile = IOUtils.toString(ElasticsearchContainer.class.getResourceAsStream("/elasticsearch/Dockerfile"),
					Charset.defaultCharset());
				dockerFile = dockerFile.replace("%VERSION%", VERSION);
				dockerImage.withFileFromString("Dockerfile", dockerFile);
				return dockerImage;
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			return new RemoteDockerImage("docker.elastic.co/elasticsearch/elasticsearch-oss:" + VERSION);
		}
	}

	@Override
	protected void configure() {
		addEnv("discovery.type", "single-node");

		if (!withIngest) {
			addEnv("node.ingest", "false");
		}

		// addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofSeconds(250L));
		waitingFor(Wait.forHttp("/"));
	}

	public ElasticsearchContainer withIngest() {
		this.withIngest = true;
		return this;
	}

}
