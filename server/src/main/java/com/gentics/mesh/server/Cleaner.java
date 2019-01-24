package com.gentics.mesh.server;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.parameter.client.NodeParametersImpl;
import com.gentics.mesh.parameter.client.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.vertx.core.Vertx;

public class Cleaner {

	private static final String PROJECT_NAME = "kofl";

	public static void main(String[] args) {
		MeshRestClient client = MeshRestClient.create("localhost", Vertx.vertx());

		client.setLogin("admin", "admin");
		client.login().blockingGet();

		for (int i = 1; i < Integer.MAX_VALUE; i++) {
			System.out.println("Loading page {" + i + "}");
			NodeListResponse list = client
				.findNodeChildren(PROJECT_NAME, "480ef3f40a324dbf8ef3f40a329dbf85", new NodeParametersImpl().setLanguages("de"),
					new PagingParametersImpl().setPerPage(1000).setPage(i))
				.toSingle().blockingGet();
			if (list.getData().size() == 0) {
				break;
			}

			for (NodeResponse entry : list.getData()) {

				if (entry.getSchema().getName().equals("fl_property") && entry.getFields().hasField("importedKius")) {

					String uuid = entry.getUuid();
					System.out.println("Updating node {" + uuid + "}");
					NodeUpdateRequest updateRequest = new NodeUpdateRequest();
					// Lets explicitly delete the list
					updateRequest.getFields().put("importedKius", null);
					updateRequest.setLanguage(entry.getLanguage());
					updateRequest.setVersion(entry.getVersion());
					client.updateNode(PROJECT_NAME, uuid, updateRequest).toCompletable().blockingAwait();

					// Publish the node
					client.publishNode(PROJECT_NAME, uuid).toCompletable().blockingAwait();
				}
			}
		}

	}
}
