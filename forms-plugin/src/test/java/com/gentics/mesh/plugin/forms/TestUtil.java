package com.gentics.mesh.plugin.forms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.github.jknack.handlebars.internal.lang3.StringUtils;

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
