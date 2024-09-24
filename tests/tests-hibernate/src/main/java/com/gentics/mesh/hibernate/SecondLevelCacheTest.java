package com.gentics.mesh.hibernate;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.stat.CacheRegionStatistics;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.FULL)
@Ignore("Test ignored, because caches for project and branch caches have been changed")
public class SecondLevelCacheTest extends AbstractMeshTest implements MeshOptionChanger {

	@Before
	public void setup() {
		tx(tx -> {
			SessionFactoryImplementor sessionFactory = ((HibernateTx) tx).getSessionImpl().getSessionFactory();
			sessionFactory.getCache().evictQueryRegions();
			sessionFactory.getCache().evictDefaultQueryRegion();
			sessionFactory.getCache().evictCollectionData();
			sessionFactory.getCache().evictEntityData();
			sessionFactory.getStatistics().clear();
		});
	}

	@Test
	public void testProjectCacheByUuid() {
		db().tx((tx) -> {
			// warm up second level cache
			tx.projectDao().findByUuid(projectUuid());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForEntity(tx, HibProjectImpl.class);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getPutCount()).isEqualTo(1);
			assertThat(statistics.getHitCount()).isEqualTo(0);
		});

		db().tx((tx) -> {
			// load entity
			tx.projectDao().findByUuid(projectUuid());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForEntity(tx, HibProjectImpl.class);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getHitCount()).isEqualTo(1);
		});
	}

	@Test
	public void testProjectCacheByName() {
		db().tx((tx) -> {
			// warm up second level cache
			tx.projectDao().findByName(projectName());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForEntity(tx, HibProjectImpl.class);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getPutCount()).isEqualTo(1);
			assertThat(statistics.getHitCount()).isEqualTo(0);
		});

		db().tx((tx) -> {
			// load entity
			tx.projectDao().findByName(projectName());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForEntity(tx, HibProjectImpl.class);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getHitCount()).isEqualTo(1);
		});
	}

	@Test
	public void testBranchCacheByName() {
		db().tx((tx) -> {
			// warm up second level cache
			tx.branchDao().findByName(project(), initialBranch().getName());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForQuery(tx);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getPutCount()).isEqualTo(1);
			assertThat(statistics.getHitCount()).isEqualTo(0);
		});

		db().tx((tx) -> {
			// load entity
			tx.branchDao().findByName(project(), initialBranch().getName());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForQuery(tx);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getHitCount()).isEqualTo(1);
		});
	}

	@Test
	public void testFetchNodeParent() {
		db().tx((tx) -> {
			// warm up second level cache
			tx.nodeDao().getParentNode(content(), initialBranchUuid());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForQuery(tx);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getPutCount()).isEqualTo(1);
			assertThat(statistics.getHitCount()).isEqualTo(0);
		});

		db().tx((tx) -> {
			// load entity
			tx.nodeDao().getParentNode(content(), initialBranchUuid());
			CacheRegionStatistics statistics = getCacheRegionStatisticsForQuery(tx);
			assertThat(statistics).isNotNull();
			assertThat(statistics.getHitCount()).isEqualTo(1);
		});
	}

	public CacheRegionStatistics getCacheRegionStatisticsForEntity(Tx tx, Class<?> entityClass) {
		HibernateTx hibTx = (HibernateTx) tx;
		SessionFactoryImplementor sessionFactory = hibTx.getSessionImpl().getSessionFactory();
		return sessionFactory.getStatistics().getDomainDataRegionStatistics(entityClass.getName());
	}

	public CacheRegionStatistics getCacheRegionStatisticsForQuery(Tx tx) {
		HibernateTx hibTx = (HibernateTx) tx;
		SessionFactoryImplementor sessionFactory = hibTx.getSessionImpl().getSessionFactory();
		return sessionFactory.getStatistics().getQueryRegionStatistics(RegionFactory.DEFAULT_QUERY_RESULTS_REGION_UNQUALIFIED_NAME);
	}

	@Override
	public void change(MeshOptions options) {
		HibernateMeshOptions meshOptions = (HibernateMeshOptions) options;
		meshOptions.getStorageOptions().setSecondLevelCacheEnabled(true);
		meshOptions.getStorageOptions().setGenerateStatistics(true);
	}
}
