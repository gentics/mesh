package com.gentics.mesh.hibernate.data;

import static com.gentics.mesh.contentoperation.CommonContentColumn.DB_UUID;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.query.NativeQuery;
import org.junit.Test;

import com.gentics.mesh.check.NodeFieldContainerCheck;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.core.node.AbstractMassiveNodeLoadTest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

import jakarta.persistence.EntityManager;

@MeshTestSetting(testSize = FULL, startServer = true, customOptionChanger = MissingContentRepairTest.Changer.class)
public class MissingContentRepairTest extends AbstractMassiveNodeLoadTest {

	public MissingContentRepairTest() {
		super(500);
	}

	@Test
	public void testRepair() {
		NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentFolderUuid, new VersioningParametersImpl().draft()));
		try (Tx tx = tx()) {
			DatabaseConnector databaseConnector = tx.<HibernateTx>unwrap().data().getDatabaseConnector();
			AtomicInteger ai = new AtomicInteger(0);
			Set<HibNode> victims = tx.nodeDao().findByUuids(project(), nodeList.getData().stream().map(node -> node.getUuid()).collect(Collectors.toSet()))
					.filter(node -> (ai.incrementAndGet() % 4) == 0)
					.map(Pair::getRight)
					.collect(Collectors.toSet());
			tx.contentDao().getFieldsContainers(victims, project().getLatestBranch().getUuid(), ContainerType.DRAFT).values().stream().flatMap(List::stream).forEach(content -> {
				String tableName = databaseConnector.getPhysicalTableName(content.getSchemaContainerVersion());
				String deleteStatement = String.format("delete from %s where %s = :uuid", tableName, databaseConnector.renderColumn(DB_UUID));
				EntityManager em = HibernateTx.get().entityManager();
				NativeQuery<?> query = em.createNativeQuery(deleteStatement).unwrap(NativeQuery.class);
				query.addSynchronizedQuerySpace(""); // prevent eviction of all hibernate second cache entities
				query.setParameter("uuid", content.getId());
				query.executeUpdate();
			});
			tx.success();
		}

		ConsistencyCheckResult checkResult = tx(tx -> {
			return new NodeFieldContainerCheck().invoke(db(), tx, true);
		});
		assertThat(checkResult.getResults()).isNotEmpty();
		assertThat(checkResult.getResults().get(0).getDescription()).startsWith("Removed 250 records from the table mesh_nodefieldcontainer, that reference records, which do not exist in table mesh_content_");

		checkResult = tx(tx -> {
			return new NodeFieldContainerCheck().invoke(db(), tx, false);
		});
		assertThat(checkResult.getResults()).isEmpty();
	}

	public static final class Changer implements MeshOptionChanger {

		@Override
		public void change(MeshOptions options) {
			MeshCoreOptionChanger.SHORT_CONTENT_BATCH.change(options);
			MeshCoreOptionChanger.SHORT_MIGRATION_BATCH.change(options);
		}
	}
}
