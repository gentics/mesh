package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Test;

import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.CoreTestUtils;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NameCacheTest extends AbstractMeshTest {

	public void clearCache() {
		adminCall(() -> client().clearCache());
	}

	@Test
	public void testCreateProject() {
		testCreation("project", 
				(name, tx) -> tx.projectDao().create(name, "localhost", false, "", user(), schemaContainer("folder").getLatestVersion(), data().createBatch()), 
				(name, tx) -> tx.projectDao().findByName(name));
	}

	@Test
	public void testCreateRole() {
		testCreation("role", (name, tx) -> tx.roleDao().create(name, user()), (name, tx) -> tx.roleDao().findByName(name));
	}

	@Test
	public void testCreateBranch() {
		testCreation("branch", (name, tx) -> tx.branchDao().create(project(), name, user(), data().createBatch()), (name, tx) -> tx.branchDao().findByName(project(), name));
	}

	@Test
	public void testCreateTag() {
		testCreation("tag", (name, tx) -> tx.tagDao().create(tagFamily("colors"), name, project(), user()), (name, tx) -> tx.tagDao().findByName(tagFamily("colors"), name));
	}

	@Test
	public void testCreateTagFamily() {
		testCreation("tagfamily", (name, tx) -> tx.tagFamilyDao().create(project(), name, user()), (name, tx) -> tx.tagFamilyDao().findByName(project(), name));
	}

	@Test
	public void testCreateGroup() {
		testCreation("group", (name, tx) -> tx.groupDao().create(name, user()), (name, tx) -> tx.groupDao().findByName(name));
	}

	@Test
	public void testCreateLanguage() {
		testCreation("language", (name, tx) -> tx.languageDao().create(name, "lng"), (name, tx) -> tx.languageDao().findByName(name));
	}

	@Test
	public void testCreateUser() {
		testCreation("user", (name, tx) -> tx.userDao().create(name, user()), (name, tx) -> tx.userDao().findByName(name));
	}

	@Test
	public void testCreateSchema() {
		testCreation("schema", (name, tx) -> {
			try {
				return tx.schemaDao().create(new SchemaModelImpl().setName(name), user());
			} catch (MeshSchemaException e) {
				throw new IllegalStateException(e);
			}
		}, (name, tx) -> tx.schemaDao().findByName(name));
	}

	@Test
	public void testCreateMicroschema() {
		testCreation("microschema", 
				(name, tx) -> tx.microschemaDao().create(new MicroschemaModelImpl().setName(name), user(), data().createBatch()),
				(name, tx) -> tx.microschemaDao().findByName(name));
	}

	protected <E extends HibNamedElement> void testCreation(String entityName, BiFunction<String, Tx, E> creator, BiFunction<String, Tx, E> lookup) {
		String name = entityName + "_" + Long.toHexString(System.currentTimeMillis());

		tx(tx -> {
			E entity = lookup.apply(name, tx);

			assertNull(entity);

			entity = creator.apply(name, tx);

			assertNotNull(entity);
			assertEquals(entity.getName(), name);

			entity = lookup.apply(name, tx);

			assertNotNull(entity);
			assertEquals(entity.getName(), name);
		});
	}

	@Test
	public void testProjectCreation() {
		testEntityCreation(
				name -> client().createProject(new ProjectCreateRequest().setName(name).setSchemaRef("folder")), 
				name -> client().findProjectByName(name), 
				project -> project.getName());
	}

	

	public <T> void testEntityCreation(Function<String, MeshRequest<T>> creator, Function<String, MeshRequest<T>> lookup, Function<T, String> namer) {
		String name = "bogus_" + Long.toHexString(System.currentTimeMillis());
		T entity;
		try {
			entity = call(() -> lookup.apply(name));
		} catch (Exception e) {
			if (CoreTestUtils.isResponseStatus(e.getCause(), HttpResponseStatus.NOT_FOUND)) {
				entity = null;
			} else {
				throw e;
			}
		}

		assertNull(entity);

		entity = call(() -> creator.apply(name));

		assertNotNull(entity);
		assertEquals(namer.apply(entity), name);

		entity = call(() -> lookup.apply(name));

		assertNotNull(entity);
		assertEquals(namer.apply(entity), name);
	}
}
