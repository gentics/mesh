package com.gentics.mesh.rest;

import org.junit.Test;

import com.gentics.mesh.rest.monitoring.MonitoringRestClient;

public class MonitoringClientTest {

	@Test
	public void testRestClient() {
		MonitoringRestClient client = MonitoringRestClient.create("localhost", 8081);
		client.close();
	}

}
