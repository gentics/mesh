package com.gentics.mesh.core.graphql;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.hazelcast.util.function.Consumer;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class OrientDBGraphQLEndpointTest extends GraphQLEndpointTest {

	public OrientDBGraphQLEndpointTest(String queryName, boolean withMicroschema, String version,
			Consumer<JsonObject> assertion, String apiVersion) {
		super(queryName, withMicroschema, version, assertion, apiVersion);
	}

	@Override
	protected void safelySetUuid(Tx tx, HibSchema schemaContainer, String uuid) {
		for (Vertex node : ((GraphDBTx) tx).getGraph().getVertices("schema", schemaContainer.getUuid())) {
			node.setProperty("schema", uuid);
		}
		schemaContainer.setUuid(uuid);
	}
}
