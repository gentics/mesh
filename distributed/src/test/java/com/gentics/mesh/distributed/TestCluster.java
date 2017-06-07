package com.gentics.mesh.distributed;

import org.junit.Test;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.command.LogContainerResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;

public class TestCluster {

	LogContainerResultCallback callback = new LogContainerResultCallback() {
		@Override
		public void onNext(Frame item) {
			System.out.print(new String(item.getPayload()));
		}
	};

	PullImageResultCallback pullCallback = new PullImageResultCallback() {
		
		@Override
		public void onNext(com.github.dockerjava.api.model.PullResponseItem item) {
			System.out.println(item.toString());
		}
	};

	@Test
	public void testCluster() throws InterruptedException {
		DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().withDockerHost("tcp://localhost:2375")
				.withDockerTlsVerify(true)
				// .withDockerCertPath("/home/user/.docker/certs")
				// .withDockerConfig("/home/user/.docker")
				.withApiVersion("1.23")
				// .withRegistryUrl("https://index.docker.io/v1/")
				// .withRegistryUsername("dockeruser")
				// .withRegistryPassword("ilovedocker")
				// .withRegistryEmail("dockeruser@github.com")
				.withDockerTlsVerify(false).build();
		DockerClient docker = DockerClientBuilder.getInstance(config).build();

		docker.pullImageCmd("debian:latest").exec(pullCallback).awaitCompletion();
		CreateContainerResponse container = docker.createContainerCmd("debian").withCmd("ls", "-la").exec();
		docker.startContainerCmd(container.getId()).exec();

		docker.logContainerCmd(container.getId()).withStdErr(true).withStdOut(true).withFollowStream(true).withTailAll().exec(callback);

		docker.stopContainerCmd(container.getId()).exec();
		docker.waitContainerCmd(container.getId()).exec(null);

	}
}
