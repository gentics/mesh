package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class AtomicLanguageTest extends AbstractMeshTest {

	@Test
	public void testLanguageIndex() {
		try (Tx tx = tx()) {
			MeshRoot meshRoot = boot().meshRoot();
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

}
