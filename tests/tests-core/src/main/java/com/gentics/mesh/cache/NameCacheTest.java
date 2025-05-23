package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.db.TxAction2;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.helper.ExpectedEvent;
import com.gentics.mesh.util.CoreTestUtils;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NameCacheTest extends AbstractMeshTest {

	@Before
	public void clearCache() {
		adminCall(() -> client().clearCache());
	}

	@Test
	public void testCreateProject() {
		testCreation("project", 
				(name, tx) -> tx.projectDao().create(name, "localhost", false, "", user(), schemaContainer("folder").getLatestVersion(), tx.batch()), 
				(name, tx) -> tx.projectDao().findByName(name), MeshEvent.PROJECT_CREATED);
	}

	@Test
	public void testCreateRole() {
		testCreation("role", (name, tx) -> tx.roleDao().create(name, user()), (name, tx) -> tx.roleDao().findByName(name), MeshEvent.ROLE_CREATED);
	}

	@Test
	public void testCreateBranch() {
		testCreation("branch", (name, tx) -> tx.branchDao().create(project(), name, user(), tx.batch()), (name, tx) -> tx.branchDao().findByName(project(), name), MeshEvent.BRANCH_CREATED);
	}

	@Test
	public void testCreateTag() {
		testCreation("tag", (name, tx) -> tx.tagDao().create(tagFamily("colors"), name, project(), user()), (name, tx) -> tx.tagDao().findByName(tagFamily("colors"), name), MeshEvent.TAG_CREATED);
	}

	@Test
	public void testCreateTagFamily() {
		testCreation("tagfamily", (name, tx) -> tx.tagFamilyDao().create(project(), name, user()), (name, tx) -> tx.tagFamilyDao().findByName(project(), name), MeshEvent.TAG_FAMILY_CREATED);
	}

	@Test
	public void testCreateGroup() {
		testCreation("group", (name, tx) -> tx.groupDao().create(name, user()), (name, tx) -> tx.groupDao().findByName(name), MeshEvent.GROUP_CREATED);
	}

	@Test
	public void testCreateLanguage() {
		testCreation("language", (name, tx) -> tx.languageDao().create(name, "lng"), (name, tx) -> tx.languageDao().findByName(name), null);
	}

	@Test
	public void testCreateUser() {
		testCreation("user", (name, tx) -> tx.userDao().create(name, user()), (name, tx) -> tx.userDao().findByName(name), MeshEvent.USER_CREATED);
	}

	@Test
	public void testCreateSchema() {
		testCreation("schema", (name, tx) -> {
			try {
				return tx.schemaDao().create(new SchemaModelImpl().setName(name), user());
			} catch (MeshSchemaException e) {
				throw new IllegalStateException(e);
			}
		}, (name, tx) -> tx.schemaDao().findByName(name), MeshEvent.SCHEMA_CREATED);
	}

	@Test
	public void testCreateMicroschema() {
		testCreation("microschema", 
				(name, tx) -> tx.microschemaDao().create(new MicroschemaModelImpl().setName(name), user(), tx.batch()),
				(name, tx) -> tx.microschemaDao().findByName(name), MeshEvent.MICROSCHEMA_CREATED);
	}

	/**
	 * <ol>
	 * <li>Lookup the entity by name (expecting to get null)</li>
	 * <li>Create the entity</li>
	 * <li>Lookup the entity by name again (expecting to get the created entity)</li>
	 * </ol>
	 * @param <E> type of the tested entity
	 * @param entityName entity name
	 * @param creator function to create the entity
	 * @param lookup function to lookup the entity by name
	 * @param creationEvent event which is expected to be fired (may be null)
	 */
	protected <E extends HibNamedElement> void testCreation(String entityName, BiFunction<String, Tx, E> creator, BiFunction<String, Tx, E> lookup, MeshEvent creationEvent) {
		String name = entityName + "_" + Long.toHexString(System.currentTimeMillis());

		tx(tx -> {
			E entity = lookup.apply(name, tx);

			assertThat(entity).as("Entity with name '%s' before creation".formatted(name)).isNull();
		});

		TxAction2 create = tx -> {
			E entity = creator.apply(name, tx);

			assertThat(entity).as("Created entity").isNotNull().hasFieldOrPropertyWithValue("name", name);
			tx.success();
		};

		if (creationEvent != null) {
			try (ExpectedEvent ee = expectEvent(creationEvent, 10_000)) {
				tx(create);
			} catch (TimeoutException e) {
				fail("Timeout while waiting for event %s".formatted(creationEvent));
			}
		} else {
			tx(create);
		}

		tx(tx -> {
			E entity = lookup.apply(name, tx);

			assertThat(entity).as("Entity with name '%s' after creation".formatted(name)).isNotNull().hasFieldOrPropertyWithValue("name", name);
		});
	}

	@Test
	public void testProjectCreation() {
		testEntityCreation(
				name -> client().createProject(new ProjectCreateRequest().setName(name).setSchemaRef("folder")), 
				name -> client().findProjectByName(name), 
				project -> project.getName(), MeshEvent.PROJECT_CREATED);
	}

	/**
	 * <ol>
	 * <li>Lookup the entity by name (expecting to get null or "not found")</li>
	 * <li>Create the entity</li>
	 * <li>Lookup the entity by name again (expecting to get the created entity)</li>
	 * </ol>
	 * @param <T> type of the tested entity
	 * @param creator function to create the entity
	 * @param lookup function to lookup the entity by name
	 * @param namer function to get the name of the entity
	 * @param creationEvent event which is expected to be fired (may be null)
	 */
	public <T> void testEntityCreation(Function<String, MeshRequest<T>> creator, Function<String, MeshRequest<T>> lookup, Function<T, String> namer, MeshEvent creationEvent) {
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

		assertThat(entity).as("Entity with name '%s' before creation".formatted(name)).isNull();

		if (creationEvent != null) {
			try (ExpectedEvent ee = expectEvent(creationEvent, 10_000)) {
				entity = call(() -> creator.apply(name));
			} catch (TimeoutException e) {
				fail("Timeout while waiting for event %s".formatted(creationEvent));
			}
		} else {
			entity = call(() -> creator.apply(name));
		}

		assertThat(entity).as("Created entity").isNotNull().hasFieldOrPropertyWithValue("name", name);

		entity = call(() -> lookup.apply(name));

		assertThat(entity).as("Entity with name '%s' after creation".formatted(name)).isNotNull().hasFieldOrPropertyWithValue("name", name);
	}
}
