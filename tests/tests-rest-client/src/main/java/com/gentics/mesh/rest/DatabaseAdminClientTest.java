package com.gentics.mesh.rest;

import org.junit.Test;

import com.gentics.mesh.rest.dbadmin.DatabaseAdminRestClient;

public class DatabaseAdminClientTest {

	@Test
	public void testRestClient() {
		DatabaseAdminRestClient client = DatabaseAdminRestClient.create("localhost", 8082);
		client.close();
	}

}
