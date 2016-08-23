package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;

public class MicroschemaSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	private MicroschemaVerticle microschemaVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
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
