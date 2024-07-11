package com.gentics.mesh.core.tag;

import static com.gentics.mesh.test.ElasticsearchTestMode.NONE;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.data.user.User;
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

			User user = userDao.create("test", null);
			LanguageDao languageRoot = tx.languageDao();
			assertNotNull(languageRoot);

			Project project = project();
			TagFamily tagFamily = tx.tagFamilyDao().create(project, "basic", user);

			Tag tag = tagDao.create(tagFamily, "dummyName", project, user);
			String uuid = tag.getUuid();
			assertNotNull(tag);
			assertEquals("dummyName", tag.getName());
			tag.setName("renamed tag");
			assertEquals("renamed tag", tag.getName());

			Tag reloadedTag = tx.tagDao().findByUuid(uuid);
			assertNotNull(reloadedTag);
			assertEquals("renamed tag", reloadedTag.getName());
		}
	}
}
