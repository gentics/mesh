package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class AtomicLanguageTest extends AbstractMeshTest {

	@Test
	public void testLanguageIndex() {
		try (Tx tx = tx()) {
			LanguageDao languageRoot = boot().languageDao();
			try (Tx tx2 = tx()) {
				assertNotNull(languageRoot);
				HibLanguage lang = languageRoot.create("Deutsch1", "de1");
				lang = languageRoot.create("English1", "en1");
				tx2.success();
			}

			assertNotNull(languageRoot.findByLanguageTag("en1"));
		}
	}
}
