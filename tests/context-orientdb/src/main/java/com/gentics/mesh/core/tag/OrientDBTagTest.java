package com.gentics.mesh.core.tag;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;

public class OrientDBTagTest extends AbstractMeshTest {

	@Test
	public void testRootNode() {
		try (Tx tx = tx()) {
			TagRoot root = ((OrientDBBootstrapInitializer) boot()).meshRoot().getTagRoot();
			assertEquals(tags().size(), root.computeCount());
			HibTag tag = tag("red");
			root.removeTag(tag);
			assertEquals(tags().size() - 1, root.computeCount());
			root.removeTag(tag);
			assertEquals(tags().size() - 1, root.computeCount());
			root.addTag(tag);
			assertEquals(tags().size(), root.computeCount());
			root.addTag(tag);
			assertEquals(tags().size(), root.computeCount());
			root.delete(createBulkContext());
		}
	}
}
