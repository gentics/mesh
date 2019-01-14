package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
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
import com.gentics.mesh.core.endpoint.migration.micronode.MicronodeMigrationHandler;
import com.gentics.mesh.core.endpoint.migration.node.NodeMigrationHandler;
import com.gentics.mesh.core.field.DataAsserter;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.core.field.FieldSchemaCreator;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.exceptions.CompositeException;

/**
 * Base class for all field migration tests
 */
public abstract class AbstractFieldMigrationTest extends AbstractMeshTest implements FieldMigrationTestcases {

	protected final static String NEWFIELD = "New field";
	protected final static String NEWFIELDVALUE = "New field value";
	protected final static String OLDFIELD = "Old field";
	protected final static String OLDFIELDVALUE = "Old field value";

	protected final static String INVALIDSCRIPT = "this is an invalid script";

	protected final static String KILLERSCRIPT = "function migrate(node, fieldname) {var System = Java.type('java.lang.System'); System.exit(0);}";

	protected NodeMigrationHandler nodeMigrationHandler;

	protected MicronodeMigrationHandler micronodeMigrationHandler;

	@Before
	public void setupDeps() {
		this.nodeMigrationHandler = meshDagger().nodeMigrationHandler();
		this.micronodeMigrationHandler = meshDagger().micronodeMigrationHandler();
	}

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
	protected void removeField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
			ExecutionException, TimeoutException {
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				removeMicroschemaField(creator, dataProvider, fetcher);
			} else {
				removeSchemaField(creator, dataProvider, fetcher);
			}
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
	private void removeSchemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
			ExecutionException, TimeoutException {
		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		SchemaContainer container = Tx.getActive().getGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		container.setCreated(user());
		boot().schemaContainerRoot().addItem(container);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, "1.0", creator.create(persistentFieldName), creator.create(
				removedFieldName));
		container.setLatestVersion(versionA);

		// create version 2 of the schema (with one field removed)
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, "2.0", creator.create(persistentFieldName));

		// link the schemas with the change in between
		RemoveFieldChange change = Tx.getActive().getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestBranch().assignSchemaVersion(user(), versionA);
		User user = user();
		String english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user);
		dataProvider.set(englishContainer, persistentFieldName);
		dataProvider.set(englishContainer, removedFieldName);

		// migrate the node
		project().getLatestBranch().assignSchemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		nodeMigrationHandler.migrateNodes(new NodeMigrationActionContextImpl(), project(), project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait();

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
	private void removeMicroschemaField(FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
			ExecutionException, TimeoutException {
		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		MicroschemaContainer container = Tx.getActive().getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		container.setName(microschemaName);
		container.setCreated(user());
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", creator.create(persistentFieldName),
				creator.create(removedFieldName));

		// create version 2 of the microschema (with one field removed)
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", creator.create(persistentFieldName));

		// link the microschemas with the change in between
		RemoveFieldChange change = Tx.getActive().getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old microschema
		Node node = folder("2015").create(user(), schemaContainer("content").getLatestVersion(), project());
		MicronodeGraphField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, persistentFieldName,
				removedFieldName);
		NodeGraphFieldContainer oldContainer = node.getGraphFieldContainer("en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the node
		project().getLatestBranch().assignMicroschemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		micronodeMigrationHandler.migrateMicronodes(project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait(10, TimeUnit.SECONDS);

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		NodeGraphFieldContainer newContainer = node.getGraphFieldContainer("en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		MicronodeGraphField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(fetcher.fetch(newMicronodeField.getMicronode(), persistentFieldName)).as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(newMicronodeField.getMicronode(), removedFieldName)).as("Field '" + removedFieldName + "'").isNull();
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
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				renameMicroschemaField(creator, dataProvider, fetcher, asserter);
			} else {
				renameSchemaField(creator, dataProvider, fetcher, asserter);
			}
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
		SchemaContainer container = Tx.getActive().getGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		container.setCreated(user());
		boot().schemaContainerRoot().addItem(container);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, "1.0", creator.create(oldFieldName));
		container.setLatestVersion(versionA);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, "2.0", newField);

		// link the schemas with the changes in between
		AddFieldChangeImpl addFieldChange = Tx.getActive().getGraph().addFramedVertex(AddFieldChangeImpl.class);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());
		addFieldChange.setCustomMigrationScript(
				"function migrate(node, fieldname) {node.fields[fieldname] = node.fields[\"oldname\"]; return node;}");

		RemoveFieldChange removeFieldChange = Tx.getActive().getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		User user = user();
		project().getLatestBranch().assignSchemaVersion(user, versionA);
		String english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user);
		dataProvider.set(englishContainer, oldFieldName);

		// migrate the node
		project().getLatestBranch().assignSchemaVersion(user, versionB);
		Tx.getActive().getGraph().commit();
		nodeMigrationHandler.migrateNodes(new NodeMigrationActionContextImpl(), project(), project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait();

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
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		MicroschemaContainer container = Tx.getActive().getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		container.setName(microschemaName);
		container.setCreated(user());
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", creator.create(oldFieldName));

		// create version 2 of the microschema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the microschemas with the changes in between
		AddFieldChangeImpl addFieldChange = Tx.getActive().getGraph().addFramedVertex(AddFieldChangeImpl.class);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());
		addFieldChange.setCustomMigrationScript(
				"function migrate(node, fieldname) {node.fields[fieldname] = node.fields[\"oldname\"]; return node;}");

		RemoveFieldChange removeFieldChange = Tx.getActive().getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		Node node = folder("2015").create(user(), schemaContainer("content").getLatestVersion(), project());
		MicronodeGraphField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, oldFieldName);
		NodeGraphFieldContainer oldContainer = node.getGraphFieldContainer("en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the micronode
		Job job = project().getLatestBranch().assignMicroschemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		if (job != null) {
			triggerAndWaitForJob(job.getUuid());
		} else {
			micronodeMigrationHandler.migrateMicronodes(project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait(10, TimeUnit.SECONDS);
		}

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		NodeGraphFieldContainer newContainer = node.getGraphFieldContainer("en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		MicronodeGraphField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(fetcher.fetch(newMicronodeField.getMicronode(), oldFieldName)).as("Field '" + oldFieldName + "'").isNull();
		asserter.assertThat(newMicronodeField.getMicronode(), newFieldName);
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
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				changeMicroschemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
			} else {
				changeSchemaType(oldField, dataProvider, oldFieldFetcher, newField, asserter);
			}
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
		String schemaName = "schema_" + System.currentTimeMillis();

		// create version 1 of the schema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		SchemaContainer container = Tx.getActive().getGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(schemaName);
		container.setCreated(user());
		boot().schemaContainerRoot().addItem(container);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, "1.0", oldFieldSchema);
		container.setLatestVersion(versionA);

		// create version 2 of the schema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, "2.0", newFieldSchema);

		// link the schemas with the change in between
		FieldTypeChange change = Tx.getActive().getGraph().addFramedVertex(FieldTypeChangeImpl.class);
		change.setFieldName(fieldName);
		change.setType(newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setListType(((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestBranch().assignSchemaVersion(user(), versionA);
		User user = user();
		String english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user);
		dataProvider.set(englishContainer, fieldName);
		assertThat(englishContainer).isOf(versionA).hasVersion("0.1");

		assertThat(oldFieldFetcher.fetch(englishContainer, fieldName)).as(OLDFIELD).isNotNull();

		// migrate the node
		project().getLatestBranch().assignSchemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		nodeMigrationHandler.migrateNodes(new NodeMigrationActionContextImpl(), project(), project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait();
		// old container must not be changed
		assertThat(englishContainer).isOf(versionA).hasVersion("0.1");
		// assert that migration worked
		NodeGraphFieldContainer migratedContainer = node.getGraphFieldContainer("en");
		assertThat(migratedContainer).isOf(versionB).hasVersion("0.2");
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema) && !StringUtils.equals(
				((ListFieldSchema) oldFieldSchema).getListType(), ((ListFieldSchema) newFieldSchema).getListType())) {
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
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldFieldSchema = oldField.create(fieldName);
		MicroschemaContainer container = Tx.getActive().getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		container.setName(microschemaName);
		container.setCreated(user());
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldFieldSchema);

		// create version 2 of the microschema (with the field modified)
		FieldSchema newFieldSchema = newField.create(fieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newFieldSchema);

		// link the schemas with the change in between
		FieldTypeChange change = Tx.getActive().getGraph().addFramedVertex(FieldTypeChangeImpl.class);
		change.setFieldName(fieldName);
		change.setType(newFieldSchema.getType());
		if (newFieldSchema instanceof ListFieldSchema) {
			change.setListType(((ListFieldSchema) newFieldSchema).getListType());
		}
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		project().getMicroschemaContainerRoot().addMicroschema(user(), container);
		Node node = folder("2015").create(user(), schemaContainer("content").getLatestVersion(), project());

		// create a node based on the old schema
		MicronodeGraphField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, fieldName);
		NodeGraphFieldContainer oldContainer = node.getGraphFieldContainer("en");
		VersionNumber oldVersion = oldContainer.getVersion();

		assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNotNull();

		// migrate the micronode
		micronodeMigrationHandler.migrateMicronodes(project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait(10, TimeUnit.SECONDS);

		// old container must be untouched
		micronodeField = oldContainer.getMicronode(micronodeFieldName);
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old micronode").isOf(versionA);

		// assert that migration worked
		NodeGraphFieldContainer newContainer = node.getGraphFieldContainer("en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		MicronodeGraphField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);

		if (!StringUtils.equals(oldFieldSchema.getType(), newFieldSchema.getType())) {
			assertThat(oldFieldFetcher.fetch(newMicronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		if ((oldFieldSchema instanceof ListFieldSchema) && (newFieldSchema instanceof ListFieldSchema) && !StringUtils.equals(
				((ListFieldSchema) oldFieldSchema).getListType(), ((ListFieldSchema) newFieldSchema).getListType())) {
			assertThat(oldFieldFetcher.fetch(newMicronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
		}
		asserter.assertThat(newMicronodeField.getMicronode(), fieldName);
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
		try (Tx tx = tx()) {
			if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
				customMicroschemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
			} else {
				customSchemaMigrationScript(creator, dataProvider, fetcher, migrationScript, asserter);
			}
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
		SchemaContainer container = Tx.getActive().getGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		container.setCreated(user());
		boot().schemaContainerRoot().addItem(container);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, "1.0", oldField);
		container.setLatestVersion(versionA);

		// Create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, "2.0", newField);

		// Link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Tx.getActive().getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(migrationScript);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		project().getLatestBranch().assignSchemaVersion(user(), versionA);
		User user = user();
		String english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		project().getLatestBranch().assignSchemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		nodeMigrationHandler.migrateNodes(new NodeMigrationActionContextImpl(), project(), project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait();

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
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		MicroschemaContainer container = Tx.getActive().getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		container.setName(microschemaName);
		container.setCreated(user());
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Tx.getActive().getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(migrationScript);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		project().getMicroschemaContainerRoot().addMicroschema(user(), container);
		Node node = folder("2015").create(user(), schemaContainer("content").getLatestVersion(), project());
		MicronodeGraphField micronodeField = createMicronodefield(node, micronodeFieldName, versionA, dataProvider, fieldName);
		NodeGraphFieldContainer oldContainer = node.getGraphFieldContainer("en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the micronode
		project().getLatestBranch().assignMicroschemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		micronodeMigrationHandler.migrateMicronodes(project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait(10, TimeUnit.SECONDS);

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		NodeGraphFieldContainer newContainer = node.getGraphFieldContainer("en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		MicronodeGraphField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
		asserter.assertThat(newMicronodeField.getMicronode(), fieldName);
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
	 * @throws Throwable
	 */
	protected void invalidMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script) throws Throwable {
		try (Tx tx = tx()) {
			try {
				if (getClass().isAnnotationPresent(MicroschemaTest.class)) {
					invalidMicroschemaMigrationScript(creator, dataProvider, script);
				} else {
					invalidSchemaMigrationScript(creator, dataProvider, script);
				}
			} catch (CompositeException e) {
				Throwable firstError = e.getExceptions().get(0);
				if (firstError instanceof javax.script.ScriptException) {
					throw firstError;
				} else {
					Throwable nestedError = firstError.getCause();
					nestedError.printStackTrace();
					throw nestedError;
				}
			}
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
	private void invalidSchemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script) throws InterruptedException,
			ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		SchemaContainer container = Tx.getActive().getGraph().addFramedVertex(SchemaContainerImpl.class);
		container.setName(UUIDUtil.randomUUID());
		container.setCreated(user());
		boot().schemaContainerRoot().addItem(container);
		SchemaContainerVersion versionA = createSchemaVersion(container, schemaName, "1.0", oldField);
		container.setLatestVersion(versionA);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		SchemaContainerVersion versionB = createSchemaVersion(container, schemaName, "2.0", newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Tx.getActive().getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(script);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		User user = user();
		project().getLatestBranch().assignSchemaVersion(user, versionA);
		String english = english();
		Node parentNode = folder("2015");
		Node node = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		project().getLatestBranch().assignSchemaVersion(user, versionB);
		Tx.getActive().getGraph().commit();
		nodeMigrationHandler.migrateNodes(new NodeMigrationActionContextImpl(), project(), project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait();
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
	private void invalidMicroschemaMigrationScript(FieldSchemaCreator creator, DataProvider dataProvider, String script) throws InterruptedException,
			ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		MicroschemaContainer container = Tx.getActive().getGraph().addFramedVertex(MicroschemaContainerImpl.class);
		container.setName(microschemaName);
		container.setCreated(user());
		MicroschemaContainerVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldField);

		// create version 2 of the microschema
		FieldSchema newField = creator.create(fieldName);
		MicroschemaContainerVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Tx.getActive().getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(script);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		createMicronodefield(folder("2015"), micronodeFieldName, versionA, dataProvider, fieldName);

		// migrate the node
		project().getLatestBranch().assignMicroschemaVersion(user(), versionB);
		Tx.getActive().getGraph().commit();
		micronodeMigrationHandler.migrateMicronodes(project().getLatestBranch(), versionA, versionB, DummyMigrationStatus.get()).blockingAwait(10, TimeUnit.SECONDS);
	}

	/**
	 * Create a schema.
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
	protected SchemaContainerVersion createSchemaVersion(SchemaContainer container, String name, String version, FieldSchema... fields) {
		SchemaModel schema = new SchemaModelImpl();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
//		schema.setDisplayField("name");
//		schema.setSegmentField("name");
		schema.validate();

		SchemaContainerVersion containerVersion = Tx.getActive().getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
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
	protected MicroschemaContainerVersion createMicroschemaVersion(MicroschemaContainer container, String name, String version,
			FieldSchema... fields) {
		MicroschemaModel schema = new MicroschemaModelImpl();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		MicroschemaContainerVersion containerVersion = Tx.getActive().getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
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
	protected MicronodeGraphField createMicronodefield(Node node, String micronodeFieldName, MicroschemaContainerVersion schemaVersion,
			DataProvider dataProvider, String... fieldNames) {
		String english = english();

		SchemaContainerVersion latestVersion = node.getSchemaContainer().getLatestVersion();

		// Add a micronode field to the schema of the node.
		SchemaModel schema = latestVersion.getSchema();
		if (schema.getField(micronodeFieldName) == null) {
			schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
		}
		schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(schemaVersion.getName());
		latestVersion.setSchema(schema);

		NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english, node.getProject().getLatestBranch(), user());
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
