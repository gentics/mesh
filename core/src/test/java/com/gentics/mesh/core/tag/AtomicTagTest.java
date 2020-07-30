package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mda.ATx;
import com.gentics.mda.entity.AUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = false)
public class AtomicTagTest extends AbstractMeshTest {

	@Test
	public void testTagCreation() throws Exception {
		try (ATx tx = tx()) {
			MeshRoot meshRoot = boot().meshRoot();
			AUser user = tx.users().create("test", null);
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			assertNotNull(languageRoot);

			meshRoot.getTagFamilyRoot();
			meshRoot.getTagRoot();

			Project project = project();
			TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
			TagFamily tagFamily = tagFamilyRoot.create("basic", user.getDelegate());

			Tag tag = tagFamily.create("dummyName", project, user.getDelegate());
			String uuid = tag.getUuid();
			assertNotNull(tag);
			assertEquals("dummyName", tag.getName());
			tag.setName("renamed tag");
			assertEquals("renamed tag", tag.getName());

			Tag reloadedTag = boot().tagRoot().findByUuid(uuid);
			assertNotNull(reloadedTag);
			assertEquals("renamed tag", reloadedTag.getName());
		}
	}
}
