package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;

import io.vertx.core.AbstractVerticle;

public class MicroschemaSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	private MicroschemaVerticle microschemaVerticle;

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(microschemaVerticle);
		return list;
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testDocumentCreation() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		// TODO Auto-generated method stub

	}
}
