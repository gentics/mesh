package com.gentics.mesh.core.language;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.test.AbstractDBTest;

public class AtomicLanguageTest extends AbstractDBTest {

	@Before
	@Override
	public void initDagger() throws Exception {
		super.initDagger();
	}

	@Test
	public void testLanguageIndex() {
		try (NoTx notx = db.noTx()) {
			MeshRoot meshRoot = boot.meshRoot();
			LanguageRoot languageRoot = meshRoot.getLanguageRoot();
			try (Tx tx = db.tx()) {
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
