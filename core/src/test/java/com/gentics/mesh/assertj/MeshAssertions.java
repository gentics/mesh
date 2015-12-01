package com.gentics.mesh.assertj;

import org.assertj.core.api.Assertions;

import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.search.impl.DummySearchProvider;

public class MeshAssertions extends Assertions {

	public static DummySearchProviderAssert assertThat(DummySearchProvider actual) {
		return new DummySearchProviderAssert(actual);
	}

	public static NodeResponseAssert assertThat(NodeResponse actual) {
		return new NodeResponseAssert(actual);
	}

	public static SearchQueueAssert assertThat(SearchQueue actual) {
		return new SearchQueueAssert(actual);
	}
}
