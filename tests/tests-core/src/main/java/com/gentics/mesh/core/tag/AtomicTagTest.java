package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = NONE, testSize = PROJECT, startServer = false)
public class AtomicTagTest extends AbstractMeshTest {

	@Test
	public void testTagCreation() throws Exception {
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			TagDao tagDao = tx.tagDao();

			HibUser user = userDao.create("test", null);
			LanguageDao languageRoot = boot().languageDao();
			assertNotNull(languageRoot);

			HibProject project = project();
			HibTagFamily tagFamily = tx.tagFamilyDao().create(project, "basic", user);

			HibTag tag = tagDao.create(tagFamily, "dummyName", project, user);
			String uuid = tag.getUuid();
			assertNotNull(tag);
			assertEquals("dummyName", tag.getName());
			tag.setName("renamed tag");
			assertEquals("renamed tag", tag.getName());

			HibTag reloadedTag = boot().tagDao().findByUuid(uuid);
			assertNotNull(reloadedTag);
			assertEquals("renamed tag", reloadedTag.getName());
		}
	}
}
