package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.function.BiFunction;

import org.junit.Test;

import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class NamedEntityDuplicationTest extends AbstractMeshTest {

	@Test(expected = RuntimeException.class)
	public void testDuplicateProject() {
		testDuplicate("project", (name, tx) -> Tx.get().projectDao().create(name, "localhost", false, "", user(), schemaContainer("folder").getLatestVersion(), data().createBatch()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateRole() {
		testDuplicate("role", (name, tx) -> Tx.get().roleDao().create(name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateBranch() {
		testDuplicate("branch", (name, tx) -> Tx.get().branchDao().create(project(), name, user(), data().createBatch()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateTag() {
		testDuplicate("tag", (name, tx) -> Tx.get().tagDao().create(tagFamily("colors"), name, project(), user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateTagFamily() {
		testDuplicate("tagfamily", (name, tx) -> Tx.get().tagFamilyDao().create(project(), name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateGroup() {
		testDuplicate("group", (name, tx) -> Tx.get().groupDao().create(name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateLanguage() {
		testDuplicate("language", (name, tx) -> Tx.get().languageDao().create(name, "lng"));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateSchema() {
		testDuplicate("schema", (name, tx) -> {
			try {
				return Tx.get().schemaDao().create(new SchemaModelImpl().setName(name), user());
			} catch (MeshSchemaException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateMicroschema() {
		testDuplicate("microschema", (name, tx) -> Tx.get().microschemaDao().create(new MicroschemaModelImpl().setName(name), user(), data().createBatch()));
	}

	protected <E extends HibNamedElement> void testDuplicate(String entityName, BiFunction<String, Tx, E> creator) {
		String name = entityName + "_" + Long.toHexString(System.currentTimeMillis());

		tx(tx -> {
			E role = creator.apply(name, tx);
			assertNotNull(role);
			assertEquals(name, role.getName());

			role = creator.apply(name, tx);
		});
	}
}
