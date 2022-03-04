package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.util.HibClassConverter;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = PROJECT, startServer = false)
public class OrientDBAtomicLanguageTest extends AbstractMeshTest {

	@Test
	public void testLanguageIndexGraphDB() {
		try (Tx tx = tx()) {
			MeshRoot meshRoot = ((OrientDBBootstrapInitializer) boot()).meshRoot();
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			try (Tx tx2 = tx()) {
				assertNotNull(languageRoot);
				Language lang = languageRoot.create();
				lang.setName("Deutsch1");
				lang.setLanguageTag("de1");
				languageRoot.addItem(lang);
				HibClassConverter.toGraph(db()).type().setVertexType(lang.getElement(), LanguageImpl.class);
				lang = languageRoot.create();
				lang.setName("English1");
				lang.setLanguageTag("en1");
				languageRoot.addItem(lang);
				HibClassConverter.toGraph(db()).type().setVertexType(lang.getElement(), LanguageImpl.class);
				tx2.success();
			}

			assertNotNull(languageRoot.findByLanguageTag("en1"));
		}
	}
}
