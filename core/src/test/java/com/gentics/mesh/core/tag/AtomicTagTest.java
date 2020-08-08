package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = false)
public class AtomicTagTest extends AbstractMeshTest {

	@Test
	public void testTagCreation() throws Exception {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.data().userDao();
			TagDaoWrapper tagDao = tx.data().tagDao();

			User user = userDao.create("test", null);
			LanguageRoot languageRoot = boot().languageRoot();
			assertNotNull(languageRoot);

			Project project = project();
			TagFamilyRoot tagFamilyRoot = project.getTagFamilyRoot();
			TagFamily tagFamily = tagFamilyRoot.create("basic", user);

			Tag tag = tagFamily.create("dummyName", project, user);
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
