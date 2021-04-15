package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;

import org.codehaus.jettison.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicSearchCrudTestcases;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class MicroschemaSearchEndpointTest extends AbstractMultiESTest implements BasicSearchCrudTestcases {

	public MicroschemaSearchEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
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
