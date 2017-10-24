package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.rest.node.WebRootResponse;

public class WebRootResponseAssert extends AbstractAssert<WebRootResponseAssert, WebRootResponse> {

	public WebRootResponseAssert(WebRootResponse actual) {
		super(actual, WebRootResponseAssert.class);
	}

	public void hasUuid(String uuid) {
		assertEquals("The uuid of the node did not match.", uuid, actual.getNodeResponse().getUuid());
	}

}
