package com.gentics.mesh.example;

import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshRestClientConfig.Builder;

/**
 * REST client usage example code.
 */
public class MeshRestClientExample {

	/**
	 * Create a Mesh Rest Client and fetch the user information.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// Set config settings
		Builder builder = MeshRestClientConfig.newConfig();
		builder.addTrustedCA("certs/server.pem");
		builder.setClientCert("certs/alice.pem");
		builder.setClientKey("certs/alice.key");
		builder.setHost("demo.getmesh.io");
		builder.setPort(443);
		builder.setSsl(true);
		builder.setHostnameVerification(false);
		builder.setBasePath("/api/v2");

		// Create the client
		MeshRestClient client = MeshRestClient.create(builder.build());

		// Load information on the authenticated user.
		// Since we have not logged in the anonymous user will be returned.
		UserResponse meInfo = client.me().blockingGet();
		System.out.println(meInfo.toJson());
	}

}
