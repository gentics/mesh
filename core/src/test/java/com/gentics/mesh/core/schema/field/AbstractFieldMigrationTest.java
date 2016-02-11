package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

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

import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaFieldChange;
import com.gentics.mesh.core.data.schema.impl.AddFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.FieldUtil;

/**
 * Base class for all field migration tests
 */
public abstract class AbstractFieldMigrationTest extends AbstractBasicDBTest implements FieldMigrationTest {
	protected final static FieldSchemaCreator CREATEBINARY = name -> FieldUtil.createBinaryFieldSchema(name);
	protected final static FieldSchemaCreator CREATEBOOLEAN = name -> FieldUtil.createBooleanFieldSchema(name);
	protected final static FieldSchemaCreator CREATEBOOLEANLIST = name -> FieldUtil.createListFieldSchema(name, "boolean");
	protected final static FieldSchemaCreator CREATEDATE = name -> FieldUtil.createDateFieldSchema(name);
	protected final static FieldSchemaCreator CREATEDATELIST = name -> FieldUtil.createListFieldSchema(name, "date");
	protected final static FieldSchemaCreator CREATEHTML = name -> FieldUtil.createHtmlFieldSchema(name);
	protected final static FieldSchemaCreator CREATEHTMLLIST = name -> FieldUtil.createListFieldSchema(name, "html");
	protected final static FieldSchemaCreator CREATEMICRONODE = name -> {
		MicronodeFieldSchema schema = FieldUtil.createMicronodeFieldSchema(name);
		schema.setAllowedMicroSchemas(new String[] {"vcard"});
		return schema;
	};
	protected final static FieldSchemaCreator CREATEMICRONODELIST = name -> {
		ListFieldSchema schema = FieldUtil.createListFieldSchema(name, "micronode");
		schema.setAllowedSchemas(new String[] {"vcard"});
		return schema;
	};
	protected final static FieldSchemaCreator CREATENODE = name -> FieldUtil.createNodeFieldSchema(name);
	protected final static FieldSchemaCreator CREATENODELIST = name -> FieldUtil.createListFieldSchema(name, "node");
	protected final static FieldSchemaCreator CREATENUMBER = name -> FieldUtil.createNumberFieldSchema(name);
	protected final static FieldSchemaCreator CREATENUMBERLIST = name -> FieldUtil.createListFieldSchema(name, "number");
	protected final static FieldSchemaCreator CREATESTRING = name -> FieldUtil.createStringFieldSchema(name);
	protected final static FieldSchemaCreator CREATESTRINGLIST = name -> FieldUtil.createListFieldSchema(name, "string");

	protected final static String NEWFIELD = "New field";
	protected final static String NEWFIELDVALUE = "New field value";
	protected final static String OLDFIELD = "Old field";
	protected final static String OLDFIELDVALUE = "Old field value";

	@Autowired
	protected NodeMigrationHandler nodeMigrationHandler;

	/**
	 * Generic method to test migration where a field has been removed from the schema/microschema
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher field fetcher implementation
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
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher field fetcher implementation
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
		SchemaContainer containerA = createSchema(schemaName, 1, creator.create(persistentFieldName),
				creator.create(removedFieldName));

		// create version 2 of the schema (with one field removed)
		SchemaContainer containerB = createSchema(schemaName, 2, creator.create(persistentFieldName));

		// link the schemas with the change in between
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousContainer(containerA);
		change.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, persistentFieldName);
		dataProvider.set(englishContainer, removedFieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(containerB).hasTranslation("en");
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), persistentFieldName))
				.as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), removedFieldName))
				.as("Field '" + removedFieldName + "'").isNull();
	}

	/**
	 * Generic method to test micronode migration where a field has been removed from the microschema
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher field fetcher implementation
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
		MicroschemaContainer containerA = createMicroschema(microschemaName, 1, creator.create(persistentFieldName), creator.create(removedFieldName));

		// create version 2 of the microschema (with one field removed)
		MicroschemaContainer containerB = createMicroschema(microschemaName, 2, creator.create(persistentFieldName));

		// link the microschemas with the change in between
		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousContainer(containerA);
		change.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a micronode based on the old microschema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, containerA, dataProvider,
				persistentFieldName, removedFieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);

		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(containerB);
		assertThat(fetcher.fetch(micronodeField.getMicronode(), persistentFieldName))
				.as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(micronodeField.getMicronode(), removedFieldName))
				.as("Field '" + removedFieldName + "'").isNull();
	}

	/**
	 * Generic method to test node migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed.
	 * Data Migration is done with a custom migration script
	 *
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher data fetcher implementation
	 * @param asserter asserter implementation
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	protected void renameField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			renameMicroschemaField(creator, dataProvider, fetcher, asserter);
		} else {
			renameSchemaField(creator, dataProvider, fetcher, asserter);
		}
	}

	/**
	 * Generic method to test node migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed.
	 * Data Migration is done with a custom migration script
	 *
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher data fetcher implementation
	 * @param asserter asserter implementation
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	private void renameSchemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		SchemaContainer containerA = createSchema(schemaName, 1, creator.create(oldFieldName));

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		SchemaContainer containerB = createSchema(schemaName, 2, newField);

		// link the schemas with the changes in between
		AddFieldChangeImpl addFieldChange = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());
		addFieldChange.setCustomMigrationScript("function migrate(node, fieldname) {node.fields[fieldname] = node.fields[\"oldname\"]; return node;}");

		RemoveFieldChange removeFieldChange = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainer(containerA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, oldFieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(containerB).hasTranslation("en");
		assertThat(fetcher.fetch(node.getGraphFieldContainer("en"), oldFieldName)).as("Field '" + oldFieldName + "'")
				.isNull();
		asserter.assertThat(node.getGraphFieldContainer("en"), newFieldName);
	}

	/**
	 * Generic method to test micronode migration where a field is renamed. Actually a new field is added (with the new name) and the old field is removed.
	 * Data Migration is done with a custom migration script
	 *
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher data fetcher implementation
	 * @param asserter asserter implementation
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 * @throws IOException
	 */
	private void renameMicroschemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher,
			DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		MicroschemaContainer containerA = createMicroschema(microschemaName, 1, creator.create(oldFieldName));

		// create version 2 of the microschema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		MicroschemaContainer containerB = createMicroschema(microschemaName, 2, newField);

		// link the microschemas with the changes in between
		AddFieldChangeImpl addFieldChange = Database.getThreadLocalGraph().addFramedVertex(AddFieldChangeImpl.class);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());
		addFieldChange.setCustomMigrationScript("function migrate(node, fieldname) {node.fields[fieldname] = node.fields[\"oldname\"]; return node;}");

		RemoveFieldChange removeFieldChange = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainer(containerA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, containerA, dataProvider,
				oldFieldName);

		// migrate the micronode
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(containerB);
		assertThat(fetcher.fetch(micronodeField.getMicronode(), oldFieldName)).as("Field '" + oldFieldName + "'")
				.isNull();
		asserter.assertThat(micronodeField.getMicronode(), newFieldName);
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * @param oldField creator for the old field
	 * @param dataProvider data provider for the old field
	 * @param oldFieldFetcher field fetcher for the old field
	 * @param newField creator for the new field
	 * @param asserter
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	protected void changeType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher,
			FieldSchemaCreator newField, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			changeMicroschemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
		} else {
			changeSchemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
		}
	}

	/**
	 * Generic method to test node migration where the type of a field is changed
	 * @param oldField creator for the old field
	 * @param dataProvider data provider for the old field
	 * @param oldFieldFetcher field fetcher for the old field
	 * @param newField creator for the new field
	 * @param asserter
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void changeSchemaType(FieldSchemaCreator oldField, DataProvider dataProvider, FieldFetcher oldFieldFetcher,
			FieldSchemaCreator newField, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "changedfield";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		SchemaContainer containerA = createSchema(schemaName, 1, oldFieldSchema);

		// create version 2 of the schema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		SchemaContainer containerB = createSchema(schemaName, 2, newFieldSchema);

		// link the schemas with the change in between
		SchemaFieldChange change = Database.getThreadLocalGraph().addFramedVertex(FieldTypeChangeImpl.class);
		change.setFieldName(fieldName);
		change.setFieldProperty("type", newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setFieldProperty("listType", ((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainer(containerA);
		change.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, fieldName);

		assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNotNull();

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(containerB).hasTranslation("en");

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema)
				&& !StringUtils.equals(((ListFieldSchema) oldFieldSchema).getListType(),
						((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(englishContainer, fieldName);
	}

	/**
	 * Generic method to test micronode migration where the type of a field is changed
	 * @param oldField creator for the old field
	 * @param dataProvider data provider for the old field
	 * @param oldFieldFetcher field fetcher for the old field
	 * @param newField creator for the new field
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
		MicroschemaContainer containerA = createMicroschema(microschemaName, 1, oldFieldSchema);

		// create version 2 of the microschema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		MicroschemaContainer containerB = createMicroschema(microschemaName, 2, newFieldSchema);

		// link the schemas with the change in between
		SchemaFieldChange change = Database.getThreadLocalGraph().addFramedVertex(FieldTypeChangeImpl.class);
		change.setFieldName(fieldName);
		change.setFieldProperty("type", newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setFieldProperty("listType", ((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainer(containerA);
		change.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, containerA, dataProvider, fieldName);

		assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNotNull();

		// migrate the micronode
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);

		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(containerB);

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema)
				&& !StringUtils.equals(((ListFieldSchema) oldFieldSchema).getListType(),
						((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(micronodeField.getMicronode(), fieldName);
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher fetcher implementation
	 * @param migrationScript migration script to test
	 * @param asserter assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	protected void customMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher,
			String migrationScript, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			customMicroschemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
		} else {
			customSchemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
		}
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher fetcher implementation
	 * @param migrationScript migration script to test
	 * @param asserter assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void customSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher,
			String migrationScript, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		SchemaContainer containerA = createSchema(schemaName, 1, oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		SchemaContainer containerB = createSchema(schemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(migrationScript);

		updateFieldChange.setPreviousContainer(containerA);
		updateFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
		node.reload();
		node.getGraphFieldContainer("en").reload();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(containerB).hasTranslation("en");
		asserter.assertThat(node.getGraphFieldContainer("en"), fieldName);
	}

	/**
	 * Generic test for migrating an existing field with a custom migration script
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @param fetcher fetcher implementation
	 * @param migrationScript migration script to test
	 * @param asserter assert implementation
	 * @throws TimeoutException
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void customMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher,
			String migrationScript, DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		MicroschemaContainer containerA = createMicroschema(microschemaName, 1, oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		MicroschemaContainer containerB = createMicroschema(microschemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(migrationScript);

		updateFieldChange.setPreviousContainer(containerA);
		updateFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a micronode based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(micronodeFieldName, containerA, dataProvider, fieldName);

		// migrate the micronode
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);

		micronodeField.getMicronode().reload();

		// assert that migration worked
		assertThat(micronodeField.getMicronode()).as("Migrated Micronode").isOf(containerB);
		asserter.assertThat(micronodeField.getMicronode(), fieldName);
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	protected void invalidMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider) throws InterruptedException, ExecutionException, TimeoutException {
		if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
			invalidMicroschemaMigrationScript(creator, dataProvider);
		} else {
			invalidSchemaMigrationScript(creator, dataProvider);
		}
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void invalidSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		SchemaContainer containerA = createSchema(schemaName, 1, oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		SchemaContainer containerB = createSchema(schemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript("this is an invalid script");

		updateFieldChange.setPreviousContainer(containerA);
		updateFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateNodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
	}

	/**
	 * Generic method to test migration failure when using an invalid migration script
	 *
	 * @param creator creator implementation
	 * @param dataProvider data provider implementation
	 * @throws TimeoutException 
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	private void invalidMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider) throws InterruptedException, ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = "migratedSchema";
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		MicroschemaContainer containerA = createMicroschema(microschemaName, 1, oldField);

		// create version 2 of the microschema
		FieldSchema newField = creator.create(fieldName);
		MicroschemaContainer containerB = createMicroschema(microschemaName, 2, newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript("this is an invalid script");

		updateFieldChange.setPreviousContainer(containerA);
		updateFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a micronode based on the old schema
		createMicronodefield(micronodeFieldName, containerA, dataProvider, fieldName);

		// migrate the node
		CompletableFuture<Void> future = new CompletableFuture<>();
		nodeMigrationHandler.migrateMicronodes(containerA, null).subscribe((item) -> {
		} , (e) -> future.completeExceptionally(e), () -> future.complete(null));
		future.get(10, TimeUnit.SECONDS);
	}

	/**
	 * Create a schema
	 * @param name schema name
	 * @param version schema version
	 * @param fields list of schema fields
	 * @return schema container
	 */
	protected SchemaContainer createSchema(String name, int version, FieldSchema...fields) {
		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schema = new SchemaImpl();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		schema.setDisplayField("name");
		schema.setSegmentField("name");
		container.setName(name);
		container.setSchema(schema);
		return container;
	}

	/**
	 * Create a microschema
	 * @param name name
	 * @param version version
	 * @param fields list of schema fields
	 * @return microschema container
	 */
	protected MicroschemaContainer createMicroschema(String name, int version, FieldSchema...fields) {
		MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		Microschema schema = new MicroschemaImpl();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		container.setName(name);
		container.setMicroschema(schema);
		return container;
	}

	/**
	 * Create a micronode field in an existing node
	 * @param micronodeFieldName name of the micronode field
	 * @param container microschema container
	 * @param dataProvider data provider
	 * @param fieldNames field names to fill
	 * @return micronode field
	 */
	protected MicronodeGraphField createMicronodefield(String micronodeFieldName,
			MicroschemaContainer container, DataProvider dataProvider, String...fieldNames) {
		Language english = english();
		Node node = folder("2015");
		Schema schema = node.getSchema();
		schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
		schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(container.getName());
		node.getSchemaContainer().setSchema(schema);

		NodeGraphFieldContainer englishContainer = node.getOrCreateGraphFieldContainer(english);
		MicronodeGraphField micronodeField = englishContainer.createMicronode(micronodeFieldName, container);
		for (String fieldName : fieldNames) {
			dataProvider.set(micronodeField.getMicronode(), fieldName);
		}
		return micronodeField;
	}

	@FunctionalInterface
	protected interface FieldSchemaCreator {
		FieldSchema create(String name);
	}

	@FunctionalInterface
	protected interface DataProvider {
		void set(GraphFieldContainer container, String name);
	}

	@FunctionalInterface
	protected interface FieldFetcher {
		GraphField fetch(GraphFieldContainer container, String name);
	}

	@FunctionalInterface
	protected interface DataAsserter {
		void assertThat(GraphFieldContainer container, String name);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.TYPE})
	protected @interface MicroschemaTest {
	}
}
