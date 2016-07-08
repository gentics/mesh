package com.gentics.mesh.core.language;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class AtomicLanguageTest extends AbstractDBTest {

	@Test
	public void testLanguageIndex() {
		try (NoTrx notx = db.noTrx()) {
			MeshRoot meshRoot = boot.meshRoot();
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			try (Trx tx = db.trx()) {
				assertNotNull(languageRoot);
				Language lang = languageRoot.create("Deutsch", "de");
				db.setVertexType(lang.getElement(), LanguageImpl.class);
				lang = languageRoot.create("English", "en");
				db.setVertexType(lang.getElement(), LanguageImpl.class);
				tx.success();
			}

			assertNotNull(languageRoot.findByLanguageTag("en"));
		}
	}

}
