package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class TagFamilyTest extends AbstractMeshTest {

	@Test
	public void testDelete() {
		TagFamily t = tagFamily("colors");
		int nTags = tx(() -> t.findAll().size());

		try (Tx tx = tx()) {
			for (int i = 0; i < 200; i++) {
				t.create("green" + i, project(), user());
			}
			tx.success();
		}
		try (Tx tx = tx()) {
			BulkActionContext bac = createBulkContext();
			t.delete(bac);
			bac.process(true);
		}
		assertThat(trackingSearchProvider()).recordedDeleteEvents(nTags + 200 + 1);
	}
}
