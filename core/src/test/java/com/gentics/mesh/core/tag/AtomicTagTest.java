package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.NONE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = false)
public class AtomicTagTest extends AbstractMeshTest {

	@Test
	public void testTagCreation() throws Exception {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			TagDaoWrapper tagDao = tx.tagDao();

			HibUser user = userDao.create("test", null);
			LanguageRoot languageRoot = boot().languageRoot();
			assertNotNull(languageRoot);

			HibProject project = project();
			HibTagFamily tagFamily = tx.tagFamilyDao().create(project, "basic", user);

			HibTag tag = tagDao.create(tagFamily, "dummyName", project, user);
			String uuid = tag.getUuid();
			assertNotNull(tag);
			assertEquals("dummyName", tag.getName());
			tag.setName("renamed tag");
			assertEquals("renamed tag", tag.getName());

			HibTag reloadedTag = boot().tagRoot().findByUuid(uuid);
			assertNotNull(reloadedTag);
			assertEquals("renamed tag", reloadedTag.getName());
		}
	}
}
