package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.function.BiFunction;

import org.junit.Test;

import com.gentics.mesh.core.data.NamedElement;
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
		testDuplicate("project", (name, tx) -> tx.projectDao().create(name, "localhost", false, "", user(), schemaContainer("folder").getLatestVersion(), data().createBatch()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateRole() {
		testDuplicate("role", (name, tx) -> tx.roleDao().create(name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateBranch() {
		testDuplicate("branch", (name, tx) -> tx.branchDao().create(project(), name, user(), data().createBatch()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateTag() {
		testDuplicate("tag", (name, tx) -> tx.tagDao().create(tagFamily("colors"), name, project(), user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateTagFamily() {
		testDuplicate("tagfamily", (name, tx) -> tx.tagFamilyDao().create(project(), name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateGroup() {
		testDuplicate("group", (name, tx) -> tx.groupDao().create(name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateLanguage() {
		testDuplicate("language", (name, tx) -> tx.languageDao().create(name, "lng"));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateUser() {
		testDuplicate("user", (name, tx) -> tx.userDao().create(name, user()));
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateSchema() {
		testDuplicate("schema", (name, tx) -> {
			try {
				return tx.schemaDao().create(new SchemaModelImpl().setName(name), user());
			} catch (MeshSchemaException e) {
				throw new IllegalStateException(e);
			}
		});
	}

	@Test(expected = RuntimeException.class)
	public void testDuplicateMicroschema() {
		testDuplicate("microschema", (name, tx) -> tx.microschemaDao().create(new MicroschemaModelImpl().setName(name), user(), data().createBatch()));
	}

	protected <E extends NamedElement> void testDuplicate(String entityName, BiFunction<String, Tx, E> creator) {
		String name = entityName + "_" + Long.toHexString(System.currentTimeMillis());

		tx(tx -> {
			E entity = creator.apply(name, tx);
			assertNotNull(entity);
			assertEquals(name, entity.getName());

			entity = creator.apply(name, tx);
		});
	}
}
