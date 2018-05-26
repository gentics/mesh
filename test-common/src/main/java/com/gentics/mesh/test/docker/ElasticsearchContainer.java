package com.gentics.mesh.test.docker;

import java.nio.charset.Charset;
import java.time.Duration;

import org.apache.commons.io.IOUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

/**
 * Testcontainer for a non-clustered Elasticsearch instance.
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

	public static final String VERSION = "6.1.2";

	private static ImageFromDockerfile image = prepareDockerImage(true);

	public ElasticsearchContainer() {
		super(image);
	}

	private static ImageFromDockerfile prepareDockerImage(boolean b) {
		try {
			ImageFromDockerfile dockerImage = new ImageFromDockerfile("elasticsearch", false);
			String dockerFile = IOUtils.toString(ElasticsearchContainer.class.getResourceAsStream("/elasticsearch/Dockerfile"), Charset.defaultCharset());
			dockerFile = dockerFile.replace("%VERSION%", VERSION);
			dockerImage.withFileFromString("Dockerfile", dockerFile);
			return dockerImage;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void configure() {
		addEnv("discovery.type", "single-node");
		//addEnv("xpack.security.enabled", "false");
		withExposedPorts(9200);
		withStartupTimeout(Duration.ofSeconds(250L));
		waitingFor(Wait.forHttp("/"));
	}

}
