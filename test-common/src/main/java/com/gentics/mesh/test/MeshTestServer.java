package com.gentics.mesh.test;

import org.junit.rules.TestRule;

import com.gentics.mesh.rest.client.MeshRestClient;

public interface MeshTestServer extends TestRule {

	/**
	 * Return the server port.
	 * 
	 * @return
	 */
	int getPort();

	/**
	 * Return the server hostname.
	 * 
	 * @return
	 */
	String getHostname();

	/**
	 * Return the mesh client for the server.
	 * 
	 * @return
	 */
	MeshRestClient client();

}
