package com.gentics.mesh.rest;

import org.junit.Test;

import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagResponse;

public class MeshRestClientTest {

	
	
	@Test
	public void testRestClient() {
		MeshRestClient client = new MeshRestClient("localhost", 8080);
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		client.createTag(tagCreateRequest).setHandler(rh -> {
			client.findTag(rh.result().getUuid()).setHandler(rh2-> {
				TagResponse tag = rh2.result();
			});
		});
		
	}
}
