package com.gentics.mesh.database;

import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.junit.Test;
import org.mockito.Mockito;

import com.gentics.mesh.contentoperation.ContentKey;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(testSize = TestSize.PROJECT)
public class DatabaseLimitsTest extends AbstractMeshTest {

	protected void testLimitParameters(Function<Integer, Integer> limitModifier) {
		Set<ContentKey> contentKeys = tx(tx -> {
			BranchDao branchDao = tx.branchDao();
			int limit = limitModifier.apply(HibernateUtil.inQueriesLimit());			
			EventQueueBatch batch = Mockito.mock(EventQueueBatch.class);
			Branch branch = branchDao.findByUuid(initialBranch().getProject(), initialBranch().getUuid());
			SchemaVersion version = schemaContainers().entrySet().stream().findAny().get().getValue().getLatestVersion();
			branchDao.assignSchemaVersion(branch, user(), version, batch);
			Set<ContentKey> edges = LongStream.range(0, limit)
					.mapToObj(i -> new ContentKey(UUIDUtil.toJavaUuid(UUIDUtil.randomUUID()), (UUID) version.getId(), ReferenceType.FIELD))
					.collect(Collectors.toSet());
			tx.success();
			System.err.println("Total nodes created: " + edges.size());
			return edges;
		});
		long total = contentKeys.size();
		System.err.println("Total nodes requested: " + total);
		tx(tx -> {
			HibernateTxImpl htx = tx.unwrap();
			long fetched = htx.getContentStorage().findMany(contentKeys).size();
			System.err.println("Total nodes fetched: " + fetched);
		});
	}

	@Test
	public void testParamsFitLimit() {
		testLimitParameters(limit -> {
			if (limit >= Integer.MAX_VALUE) {
				return Integer.MAX_VALUE - 1;
			} else {
				return limit - 1;
			}
		});
	}

	@Test
	public void testParamsOverflowLimit() {
		testLimitParameters(limit -> {
			if (limit > Integer.MAX_VALUE) {
				return Integer.MAX_VALUE;
			} else {
				return limit + 1;
			}
		});
	}
}
