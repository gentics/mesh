package com.gentics.mesh.test.docker;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

import org.junit.ClassRule;
import org.junit.Test;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class ElasticsearchContainerTest {

	static {
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	@ClassRule
	public static ElasticsearchContainer server = new ElasticsearchContainer(true);

	@Test
	public void testContainer() throws Exception {
		System.out.println(getStatus());
		server.dropTraffic();
		try {
			System.out.println(getStatus());
			fail("A timeout should happen");
		} catch (SocketTimeoutException e) {
			// ignored
		}
		server.resumeTraffic();
		System.out.println(getStatus());
	}

	private String getStatus() throws IOException {
		URL testUrl = new URL("http://" + server.getHost() + ":" + server.getFirstMappedPort() + "/_cluster/health");
		URLConnection conn = testUrl.openConnection();
		conn.setConnectTimeout(1500);
		conn.setReadTimeout(1500);
		try (InputStream ins = conn.getInputStream()) {
			try (Scanner scanner = new Scanner(ins, "UTF-8")) {
				return scanner.useDelimiter("\\A").next();
			}
		}
	}
}
