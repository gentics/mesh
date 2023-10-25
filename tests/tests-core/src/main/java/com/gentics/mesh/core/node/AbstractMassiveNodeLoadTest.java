package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import org.junit.Before;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;

public abstract class AbstractMassiveNodeLoadTest extends AbstractMeshTest {

	protected final long numOfNodesPerLevel;

	public AbstractMassiveNodeLoadTest() {
		this(9000);
	}

	public AbstractMassiveNodeLoadTest(long numOfNodesPerLevel) {
		this.numOfNodesPerLevel = numOfNodesPerLevel;
	}

	@Before
	public void makeEmAll() {
		makeEmAll(numOfNodesPerLevel, tx(() -> project().getBaseNode().getUuid()));
	}

	protected void makeEmAll(long howMany, String parentNodeUuid) {
		try (Tx tx = tx()) {
			NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentNodeUuid, new VersioningParametersImpl().draft()));
			assertEquals(3, nodeList.getData().size());

			SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
			schemaReference.setName("folder");

			for (int i = 0; i < howMany; i++) {
				NodeCreateRequest create2 = new NodeCreateRequest();
				FieldMap fields = new FieldMapImpl();
				fields.put("name", FieldUtil.createStringField("Folder " + i));
				fields.put("slug", FieldUtil.createStringField("folder" + i));
				create2.setFields(fields);
				create2.setSchema(schemaReference);
				create2.setLanguage("en");
				create2.setParentNodeUuid(parentNodeUuid);
				call(() -> client().createNode(PROJECT_NAME, create2));
			}
		}
	}
}
