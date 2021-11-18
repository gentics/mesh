package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.performance.StopWatch.stopWatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.tinkerpop.blueprints.Vertex;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = false)
public class OrientDBLanguageTest extends AbstractMeshTest {

	@Test
	public void testLanguageIndex() {
		try (Tx tx = tx()) {
			stopWatch("languageindex.read", 50000, (step) -> {
				Iterable<Vertex> it = ((GraphDBTx) tx).getGraph().getVertices("LanguageImpl.languageTag", "en");
				assertTrue(it.iterator().hasNext());
				Iterable<Vertex> it2 = ((GraphDBTx) tx).getGraph().getVertices(LanguageImpl.class.getSimpleName() + "." + LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY,
					"en");
				assertTrue(it2.iterator().hasNext());
				Vertex vertex = it2.iterator().next();
				assertNotNull("The language node with languageTag 'en' could not be found.", vertex);
				assertEquals("en", vertex.getProperty(LanguageImpl.LANGUAGE_TAG_PROPERTY_KEY));
			});
		}
	}
}
