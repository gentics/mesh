package com.gentics.mesh.liquibase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.Test;

import com.gentics.mesh.MeshVersion;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

import jakarta.persistence.EntityManager;

@MeshTestSetting(testSize = TestSize.FULL, startServer = false)
public class CopyEditorTrackingIntoEdgeTest extends AbstractMeshTest {
	public final static String CHANGE_ID = "1682071146058-2";

	@Test
	public void test() throws Exception {

		tx(tx -> {
			HibernateTx hTx = tx.unwrap();
			EntityManager em = hTx.entityManager();
			String containerTableName = hTx.data().getDatabaseConnector().maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get();

			assertThat(em.createNativeQuery("DELETE FROM DATABASECHANGELOG WHERE id = ?").setParameter(1, CHANGE_ID)
					.executeUpdate()).as("Liquibase changelog entries deleted").isEqualTo(1);

			assertThat(em.createNativeQuery("UPDATE " + containerTableName + " SET edited = NULL, editor_dbuuid = NULL")
					.executeUpdate()).as("Updated records in nodefieldcontainer table").isPositive();
		});

		db().init(MeshVersion.getBuildInfo().getVersion(), "com.gentics.mesh.core.data");

		tx(tx -> {
			HibernateTx hTx = tx.unwrap();
			EntityManager em = hTx.entityManager();
			String containerTableName = hTx.data().getDatabaseConnector().maybeGetPhysicalTableName(HibNodeFieldContainerEdgeImpl.class).get();

			Object countObject = em.createNativeQuery(
					"SELECT COUNT(*) FROM " + containerTableName + " WHERE edited IS NULL OR editor_dbuuid IS NULL")
					.getSingleResult();
			if (countObject instanceof Number) {
				assertThat(Number.class.cast(countObject).longValue()).as("Records with edited or editor_dbuuid not set").isEqualTo(0);
			} else {
				fail("Unexpected count result " + countObject);
			}
		});
	}
}
