package com.gentics.mesh.test.docker;

import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	public static final String VERSION = "6.3.1";

	public ElasticsearchContainer(boolean withIngestPlugin) {
		super(prepareDockerImage(withIngestPlugin));
	}

	private static ImageFromDockerfile prepareDockerImage(boolean withIngestPlugin) {
		try {
			ImageFromDockerfile dockerImage = new ImageFromDockerfile("elasticsearch", false);
			String dockerFile = IOUtils.toString(ElasticsearchContainer.class.getResourceAsStream("/elasticsearch/Dockerfile.ingest"),
				Charset.defaultCharset());
			dockerFile = dockerFile.replace("%VERSION%", VERSION);
			if (!withIngestPlugin) {
				dockerFile = dockerFile.replace("RUN", "#RUN");
			}
			dockerImage.withFileFromString("Dockerfile", dockerFile);
			return dockerImage;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void configure() {
		addEnv("discovery.type", "single-node");
		withTmpFs(Collections.singletonMap("/usr/share/elasticsearch/data", "rw,size=64m"));
		// addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofSeconds(250L));
		waitingFor(Wait.forHttp("/"));
	}

}
