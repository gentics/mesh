package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.TagFamilyDao;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = false)
public class TagFamilyTest extends AbstractMeshTest {

	@Test
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void testDelete() {
		HibTagFamily tagFamily = tagFamily("colors");
		Long nTags = tx(tx -> {
			return tx.tagDao().findAll(tagFamily).count();
		});
		int nExtraTags = 100;
		try (Tx tx = tx()) {
			TagDao tagDao = tx.tagDao();
			for (int i = 0; i < nExtraTags; i++) {
				tagDao.create(tagFamily, "green" + i, project(), user());
			}
			tx.success();
		}
		try (Tx tx = tx()) {
			TagFamilyDao tagFamilyDao = tx.tagFamilyDao();
			BulkActionContext bac = createBulkContext();
			tagFamilyDao.delete(tagFamily, bac);
			bac.process(true);
			tx.success();
		}
		assertThat(trackingSearchProvider()).recordedDeleteEvents(nExtraTags + nTags.intValue() + 1);
	}
}
