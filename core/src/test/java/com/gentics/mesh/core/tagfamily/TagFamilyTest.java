package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.mesh.context.DeletionContext;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class TagFamilyTest extends AbstractMeshTest {

	@Test
	public void testDelete() {
		TagFamily t = tagFamily("colors");
		try (Tx tx = tx()) {
			int nTags = t.findAll().size();
			for (int i = 0; i < 200; i++) {
				t.create("green" + i, project(), user());
			}
			DeletionContext context = createDeletionContext();
			t.delete(context);
			context.process(true);
			assertThat(trackingSearchProvider()).recordedDeleteEvents(nTags + 200 + 1);
		}
	}
}
