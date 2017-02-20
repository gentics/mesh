package com.gentics.mesh.search;

import org.codehaus.jettison.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

@MeshTestSetting(useElasticsearch = true, useTinyDataset = false, startServer = true)
public class MicroschemaSearchEndpointTest extends AbstractMeshTest implements BasicSearchCrudTestcases {

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
