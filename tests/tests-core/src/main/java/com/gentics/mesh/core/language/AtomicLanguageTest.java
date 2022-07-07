package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class AtomicLanguageTest extends AbstractMeshTest {

	@Test
	public void testLanguageIndex() {
		try (Tx tx = tx()) {
			LanguageDao languageRoot = tx.languageDao();
			assertNotNull(languageRoot);
			languageRoot.create("English1", "en1");
			tx.success();
		}
		try (Tx tx = tx()) {
			LanguageDao languageRoot = tx.languageDao();
			assertNotNull(languageRoot);
			languageRoot.create("Deutsch1", "de1");
			tx.success();
		}
		try (Tx tx = tx()) {
			assertNotNull(tx.languageDao().findByLanguageTag("en1"));
		}
	}
}
