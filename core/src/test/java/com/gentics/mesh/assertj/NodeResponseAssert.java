package com.gentics.mesh.assertj;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.NodeResponse;

public class NodeResponseAssert extends AbstractAssert<NodeResponseAssert, NodeResponse> {

	protected NodeResponseAssert(NodeResponse actual) {
		super(actual, NodeResponseAssert.class);
	}

}
