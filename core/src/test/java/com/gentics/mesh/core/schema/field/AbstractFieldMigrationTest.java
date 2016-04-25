package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.AbstractBasicDBTest;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.field.DataAsserter;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.core.field.FieldSchemaCreator;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Base class for all field migration tests
 */
public abstract class AbstractFieldMigrationTest extends AbstractBasicDBTest implements FieldMigrationTest {
	protected final static String NEWFIELD = "New field";
	protected final static String NEWFIELDVALUE = "New field value";
	protected final static String OLDFIELD = "Old field";
	protected final static String OLDFIELDVALUE = "Old field value";

	protected final static String INVALIDSCRIPT = "this is an invalid script";

	protected final static String KILLERSCRIPT = "function migrate(node, fieldname) {var System = Java.type('java.lang.System'); System.exit(0);}";

	@Autowired
	protected NodeMigrationHandler nodeMigrationHandler;

	/**
	 * Generic method to test migration where a field has been removed from the schema/microschema
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            field fetcher implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void removeField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			removeMicroschemaField(creator, dataProvider, fetcher);
		} else {
			removeSchemaField(creator, dataProvider, fetcher);
		}
	}

	/**
	 * Generic method to test node migration where a field has been removed from the schema
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            field fetcher implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void removeSchemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher)
			throws InterruptedException, ExecutionException, TimeoutException {
		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, 1, creator.create(persistentFieldName),
				creator.create(removedFieldName));

		// create version 2 of the schema (with one field removed)
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, 2, creator.create(persistentFieldName));

		// link the schemas with the change in between
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestRelease().assignSchemaVersion(versionA);
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestRelease(), user);
		dataProvider.set(englishContainer, persistentFieldName);
		dataProvider.set(englishContainer, removedFieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(project(), project().getLatestRelease(), versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(node.getGraphFieldContainer("en")).as("Migrated field container").isOf(versionB);
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), persistentFieldName)).as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), removedFieldName)).as("Field '" + removedFieldName + "'").isNull();
	}

	/**
	 * Generic method to test micronode migration where a field has been removed from the microschema
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            field fetcher implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void removeMicroschemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher)
			throws InterruptedException, ExecutionException, TimeoutException {
		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, 1, creator.create(persistentFieldName),
				creator.create(removedFieldName));

		// create version 2 of the microschema (with one field removed)
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, 2, creator.create(persistentFieldName));

		// link the microschemas with the change in between
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old microschema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, versionA, dataProvider, persistentFieldName, removedFieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);

		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(fetcher.fetch(micronodeField.getMicronode(), persistentFieldName)).as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(micronodeField.getMicronode(), removedFieldName)).as("Field '" + removedFieldName + "'").isNull();
	}

	/**
	 * Generic method to test node migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed. Data
	 * Migration is done with a custom migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            data fetcher implementation
	 * @param asserter
	 *            asserter implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	protected void renameField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			renameMicroschemaField(creator, dataProvider, fetcher, asserter);
		} else {
			renameSchemaField(creator, dataProvider, fetcher, asserter);
		}
	}

	/**
	 * Generic method to test node migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed. Data
	 * Migration is done with a custom migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            data fetcher implementation
	 * @param asserter
	 *            asserter implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void renameSchemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
			throws InterruptedException, ExecutionException, TimeoutException {
		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, 1, creator.create(oldFieldName));

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, 2, newField);

		// link the schemas with the changes in between
		AddFieldChangeImpl addFieldChange = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());
		addFieldChange
				.setCustomMigrationScript("function migrate(node, fieldname) {node.fields[fieldname] = node.fields[\"oldname\"]; return node;}");

		RemoveFieldChange removeFieldChange = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestRelease().assignSchemaVersion(versionA);
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestRelease(), user);
		dataProvider.set(englishContainer, oldFieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(project(), project().getLatestRelease(), versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(node.getGraphFieldContainer("en")).as("Migrated field container").isOf(versionB);
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), oldFieldName)).as("Field '" + oldFieldName + "'").isNull();
		asserter.assertThat(node.getGraphFieldContainer("en"), newFieldName);
	}

	/**
	 * Generic method to test micronode migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed. Data
	 * Migration is done with a custom migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            data fetcher implementation
	 * @param asserter
	 *            asserter implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private void renameMicroschemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
			throws InterruptedException, ExecutionException, TimeoutException {
		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, 1, creator.create(oldFieldName));

		// create version 2 of the microschema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, 2, newField);

		// link the microschemas with the changes in between
		AddFieldChangeImpl addFieldChange = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());
		addFieldChange
				.setCustomMigrationScript("function migrate(node, fieldname) {node.fields[fieldname] = node.fields[\"oldname\"]; return node;}");

		RemoveFieldChange removeFieldChange = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, versionA, dataProvider, oldFieldName);

		// migrate the micronode
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(fetcher.fetch(micronodeField.getMicronode(), oldFieldName)).as("Field '" + oldFieldName + "'").isNull();
		asserter.assertThat(micronodeField.getMicronode(), newFieldName);
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * 
	 * @param oldField
	 *            creator for the old field
	 * @param dataProvider
	 *            data provider for the old field
	 * @param oldFieldFetcher
	 *            field fetcher for the old field
	 * @param newField
	 *            creator for the new field
	 * @param asserter
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void changeType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher, FieldSchemaCreator newField,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			changeMicroschemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
		} else {
			changeSchemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
		}
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * 
	 * @param oldField
	 *            creator for the old field
	 * @param dataProvider
	 *            data provider for the old field
	 * @param oldFieldFetcher
	 *            field fetcher for the old field
	 * @param newField
	 *            creator for the new field
	 * @param asserter
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void changeSchemaType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher, FieldSchemaCreator newField,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "changedfield";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, 1, oldFieldSchema);

		// create version 2 of the schema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, 2, newFieldSchema);

		// link the schemas with the change in between
		FieldTypeChange change = Database.getThreadLocalGraph().addFramedVertex(FieldTypeChangeImpl.class);
		change.setFieldName(fieldName);
		change.setType(newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setListType(((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestRelease().assignSchemaVersion(versionA);
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestRelease(), user);
		dataProvider.set(englishContainer, fieldName);
		assertThat(englishContainer).isOf(versionA).hasVersion("0.1");

		assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNotNull();

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(project(), project().getLatestRelease(), versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		englishContainer.reload();
		node.getGraphFieldContainer("en").reload();

		// old container must not be changed
		assertThat(englishContainer).isOf(versionA).hasVersion("0.1");
		// assert that migration worked
		NodeGraphFieldContainer migratedContainer = node.getGraphFieldContainer("en");
		assertThat(migratedContainer).isOf(versionB).hasVersion("0.2");
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema)
				&& !StringUtils.equals(((ListFieldSchema) oldFieldSchema).getListType(), ((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(migratedContainer, fieldName);
	}

	/**
	 * Generic method to test micronode migration where the type of a field is changed
	 * 
	 * @param oldField
	 *            creator for the old field
	 * @param dataProvider
	 *            data provider for the old field
	 * @param oldFieldFetcher
	 *            field fetcher for the old field
	 * @param newField
	 *            creator for the new field
	 * @param asserter
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void changeMicroschemaType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher,
			FieldSchemaCreator newField, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "changedfield";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, 1, oldFieldSchema);

		// create version 2 of the microschema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, 2, newFieldSchema);

		// link the schemas with the change in between
		FieldTypeChange change = Database.getThreadLocalGraph().addFramedVertex(FieldTypeChangeImpl.class);
		change.setFieldName(fieldName);
		change.setType(newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setListType(((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, versionA, dataProvider, fieldName);

		assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNotNull();

		// migrate the micronode
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);

		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema)
				&& !StringUtils.equals(((ListFieldSchema) oldFieldSchema).getListType(), ((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(micronodeField.getMicronode(), fieldName);
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            fetcher implementation
	 * @param migrationScript
	 *            migration script to test
	 * @param asserter
	 *            assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void customMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			customMicroschemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
		} else {
			customSchemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
		}
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            fetcher implementation
	 * @param migrationScript
	 *            migration script to test
	 * @param asserter
	 *            assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void customSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, 1, oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(migrationScript);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestRelease().assignSchemaVersion(versionA);
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestRelease(), user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(project(), project().getLatestRelease(), versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(node.getGraphFieldContainer("en")).as("Migrated field container").isOf(versionB);
		asserter.assertThat(node.getGraphFieldContainer("en"), fieldName);
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * 
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param fetcher
	 *            fetcher implementation
	 * @param migrationScript
	 *            migration script to test
	 * @param asserter
	 *            assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void customMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, 1, oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(migrationScript);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, versionA, dataProvider, fieldName);

		// migrate the micronode
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);

		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		asserter.assertThat(micronodeField.getMicronode(), fieldName);
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param script
	 *            migration script
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	protected void invalidMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script)
			throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			invalidMicroschemaMigrationScript(creator, dataProvider, script);
		} else {
			invalidSchemaMigrationScript(creator, dataProvider, script);
		}
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param script
	 *            migration script
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void invalidSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script)
			throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, 1, oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(script);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestRelease().assignSchemaVersion(versionA);
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestRelease(), user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(project(), project().getLatestRelease(), versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator
	 *            creator implementation
	 * @param dataProvider
	 *            data provider implementation
	 * @param script
	 *            script
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void invalidMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script)
			throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, 1, oldField);

		// create version 2 of the microschema
		FieldSchema newField = creator.create(fieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(script);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		createMicronodefield(micronodeFieldName, versionA, dataProvider, fieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(versionA, versionB, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
	}

	/**
	 * Create a schema
	 * 
	 * @param container
	 *            Parent schema container for versions
	 * @param name
	 *            schema name
	 * @param version
	 *            schema version
	 * @param fields
	 *            list of schema fields
	 * @return schema container
	 */
	protected SchemaContainerVersion createSchemaVersion(SchemaContainer container, String name, int version, FieldSchema... fields) {
		Schema schema = new SchemaModel();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		schema.setDisplayField("name");
		schema.setSegmentField("name");

		SchemaContainerVersion containerVersion = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		containerVersion.setName(name);
		containerVersion.setSchema(schema);
		containerVersion.setSchemaContainer(container);
		return containerVersion;
	}

	/**
	 * Create a microschema
	 * 
	 * @param name
	 *            name
	 * @param version
	 *            version
	 * @param fields
	 *            list of schema fields
	 * @return microschema container
	 */
	protected MicroschemaContainerVersion createMicroschemaVersion(MicroschemaContainer container, String name, int version, FieldSchema... fields) {
		Microschema schema = new MicroschemaModel();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		MicroschemaContainerVersion containerVersion = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
		containerVersion.setSchema(schema);
		containerVersion.setName(name);
		containerVersion.setSchemaContainer(container);
		container.setLatestVersion(containerVersion);
		return containerVersion;
	}

	/**
	 * Create a micronode field in an existing node
	 * 
	 * @param micronodeFieldName
	 *            name of the micronode field
	 * @param schemaVersion
	 *            Microschema container version
	 * @param dataProvider
	 *            data provider
	 * @param fieldNames
	 *            field names to fill
	 * @return micronode field
	 */
	protected MicronodeGraphField createMicronodefield(String micronodeFieldName, MicroschemaContainerVersion schemaVersion,
			DataProvider dataProvider, String... fieldNames) {
		Language english = english();
		Node node = folder("2015");

		// Add a micronode field to the schema of the node.
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
		schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(schemaVersion.getName());
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english,
				node.getProject().getLatestRelease(), user());
		MicronodeGraphField micronodeField = englishContainer.createMicronode(micronodeFieldName, schemaVersion);
		for (String fieldName : fieldNames) {
			dataProvider.set(micronodeField.getMicronode(), fieldName);
		}
		return micronodeField;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	protected @interface MicroschemaTest {
	}
}
