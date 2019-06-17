package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import com.gentics.mesh.rest.client.MeshWebrootResponse;
import org.assertj.core.api.AbstractAssert;

public class WebRootResponseAssert extends AbstractAssert<WebRootResponseAssert, MeshWebrootResponse> {

	public WebRootResponseAssert(MeshWebrootResponse actual) {
		super(actual, WebRootResponseAssert.class);
	}

	public void hasUuid(String uuid) {
		assertEquals("The uuid of the node did not match.", uuid, actual.getNodeResponse().getUuid());
	}

}
