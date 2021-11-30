package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = FULL, startServer = false)
public class OrientDBSchemaTest extends AbstractMeshTest {

	@Test
	public void testGetRoot() {
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			HibSchema schemaContainer = schemaDao.findByName("content");
			RootVertex<? extends HibSchema> root = HibClassConverter.toGraph(schemaContainer).getRoot();
			assertNotNull(root);
		}
	}
}
