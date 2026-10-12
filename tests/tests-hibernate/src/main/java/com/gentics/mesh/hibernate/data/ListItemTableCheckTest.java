package com.gentics.mesh.hibernate.data;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.hibernate.query.NativeQuery;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.check.StringListItemCheck;
import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import jakarta.persistence.EntityManager;

/**
 * Test cases for the list item table consistency checks, which compose native SQL queries over both the list item table and the content table.
 */
@MeshTestSetting(testSize = FULL, startServer = true)
public class ListItemTableCheckTest extends AbstractMeshTest {
	private static final String SCHEMA_NAME = "stringlisttest";

	private static final String FIELD_NAME = "stringList";

	private String nodeUuid;

	@Before
	public void setUp() throws Exception {
		SchemaCreateRequest schemaRequest = new SchemaCreateRequest();
		schemaRequest.setName(SCHEMA_NAME);
		schemaRequest.setFields(Collections.singletonList(new ListFieldSchemaImpl().setListType("string").setName(FIELD_NAME)));
		SchemaResponse schema = client().createSchema(schemaRequest).blockingGet();
		client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()).blockingAwait();

		NodeCreateRequest nodeRequest = new NodeCreateRequest();
		nodeRequest.setLanguage("en");
		nodeRequest.setSchemaName(SCHEMA_NAME);
		nodeRequest.setParentNodeUuid(tx(() -> folder("2015").getUuid()));
		FieldMapImpl fields = new FieldMapImpl();
		fields.put(FIELD_NAME, new StringFieldListImpl().setItems(Arrays.asList("one", "two")));
		nodeRequest.setFields(fields);
		NodeResponse node = client().createNode(PROJECT_NAME, nodeRequest).blockingGet();
		nodeUuid = node.getUuid();
	}

	/**
	 * Check that the string list items of a consistent node are not reported.
	 */
	@Test
	public void testCheckConsistent() {
		ConsistencyCheckResult checkResult = tx(tx -> {
			return new StringListItemCheck().invoke(db(), tx, false);
		});
		assertThat(checkResult.getResults()).isEmpty();
	}

	/**
	 * Check that the string list items, which are no longer referenced by their content, are reported and repaired.
	 */
	@Test
	public void testRepairOrphanedItems() {
		// let the content reference another list, so that the existing list items become orphaned
		try (Tx tx = tx()) {
			DatabaseConnector databaseConnector = tx.<HibernateTx>unwrap().data().getDatabaseConnector();
			HibNode node = tx.nodeDao().findByUuid(project(), nodeUuid);
			HibNodeFieldContainer content = tx.contentDao().getLatestDraftFieldContainer(node, "en");
			String tableName = databaseConnector.getPhysicalTableName(content.getSchemaContainerVersion());
			String listColumn = databaseConnector.identify(FIELD_NAME + "-list.string").render(databaseConnector.getHibernateDialect());
			String updateStatement = String.format("update %s set %s = :listUuid where %s = :uuid", tableName, listColumn, databaseConnector.renderColumn(CommonContentColumn.DB_UUID));
			EntityManager em = HibernateTx.get().entityManager();
			NativeQuery<?> query = em.createNativeQuery(updateStatement).unwrap(NativeQuery.class);
			query.addSynchronizedQuerySpace(""); // prevent eviction of all hibernate second cache entities
			query.setParameter("listUuid", UUID.randomUUID());
			query.setParameter("uuid", content.getId());
			assertThat(query.executeUpdate()).as("Updated contents").isEqualTo(1);
			tx.success();
		}

		ConsistencyCheckResult checkResult = tx(tx -> {
			return new StringListItemCheck().invoke(db(), tx, false);
		});
		assertThat(checkResult.getResults()).hasSize(1);
		assertThat(checkResult.getResults().get(0).getDescription()).startsWith("Table mesh_stringlistitem contains 2 records, that were abandoned from the records of table ");
		assertThat(checkResult.getResults().get(0).isRepaired()).isFalse();

		checkResult = tx(tx -> {
			return new StringListItemCheck().invoke(db(), tx, true);
		});
		assertThat(checkResult.getResults()).hasSize(1);
		assertThat(checkResult.getResults().get(0).isRepaired()).isTrue();

		checkResult = tx(tx -> {
			return new StringListItemCheck().invoke(db(), tx, false);
		});
		assertThat(checkResult.getResults()).isEmpty();
	}
}
