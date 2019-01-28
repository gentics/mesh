package com.gentics.mesh.search.raw;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static org.junit.Assert.assertNotNull;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class SchemaRawSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testRawSearch() throws JSONException {
		final String name = "newschema";
		SchemaResponse schema = createSchema(name);

		String query = getSimpleTermQuery("name.raw", name);

		JSONObject response = call(() -> client().searchSchemasRaw(query));
		assertNotNull(response);
		assertThat(response).has("responses[0].hits.hits[0]._id", schema.getUuid(), "The correct element was not found.");

	}

}
