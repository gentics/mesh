package com.gentics.mesh.plugin.forms;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Flowable;
import io.reactivex.Maybe;

public final class TestUtil {
	private TestUtil() {
	}

	public static Maybe<NodeResponse> findRootNode(MeshRestClient client, String project) {
		return client.findNodes(project).toSingle()
			.flatMapPublisher(nodes -> Flowable.fromIterable(nodes.getData()))
			.filter(node -> node.getSchema().getName().equals("forms_plugin"))
			.firstElement();
	}
}
