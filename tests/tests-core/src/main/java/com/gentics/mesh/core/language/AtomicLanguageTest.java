package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.dao.LanguageDao;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class AtomicLanguageTest extends AbstractMeshTest {

	// TODO remove or move into the ODB specific test set
	@Test
	public void testLanguageIndexGraphDB() {
		try (Tx tx = tx()) {
			MeshRoot meshRoot = ((OrientDBBootstrapInitializer) boot()).meshRoot();
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			try (Tx tx2 = tx()) {
				assertNotNull(languageRoot);
				Language lang = languageRoot.create("Deutsch1", "de1");
				db().type().setVertexType(lang.getElement(), LanguageImpl.class);
				lang = languageRoot.create("English1", "en1");
				db().type().setVertexType(lang.getElement(), LanguageImpl.class);
				tx2.success();
			}

			assertNotNull(languageRoot.findByLanguageTag("en1"));
		}
	}

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
