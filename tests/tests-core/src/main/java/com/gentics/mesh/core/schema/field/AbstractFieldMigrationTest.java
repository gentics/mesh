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
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;

import com.gentics.mesh.context.impl.MicronodeMigrationContextImpl;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.MicroschemaDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibAddFieldChange;
import com.gentics.mesh.core.data.schema.HibFieldTypeChange;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibRemoveFieldChange;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.HibUpdateFieldChange;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.DataAsserter;
import com.gentics.mesh.core.field.DataProvider;
import com.gentics.mesh.core.field.FieldFetcher;
import com.gentics.mesh.core.field.FieldSchemaCreator;
import com.gentics.mesh.core.field.FieldTestHelper;
import com.gentics.mesh.core.migration.MicronodeMigration;
import com.gentics.mesh.core.migration.NodeMigration;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.CoreTestUtils;
import com.gentics.mesh.util.UUIDUtil;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.exceptions.CompositeException;

/**
 * Base class for all field migration tests
 */
public abstract class AbstractFieldMigrationTest extends AbstractMeshTest implements FieldMigrationTestcases {

	protected final static String NEWFIELD = "New field";
	protected final static String NEWFIELDVALUE = "New field value";
	protected final static String NEWBINARYCHECKSTATUS = "New binary check status";
	protected final static String OLDFIELD = "Old field";
	protected final static String OLDFIELDVALUE = "Old field value";

	protected final static String INVALIDSCRIPT = "this is an invalid script";

	protected final static String KILLERSCRIPT = "function migrate(node, fieldname) {var System = Java.type('java.lang.System'); System.exit(0);}";

	protected NodeMigration nodeMigrationHandler;

	protected MicronodeMigration micronodeMigrationHandler;

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
				removeMicroschemaField(tx, creator, dataProvider, fetcher);
			} else {
				removeSchemaField(tx, creator, dataProvider, fetcher);
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
	private void removeSchemaField(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
		ExecutionException, TimeoutException {
		NodeDao nodeDao = tx.nodeDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) tx.schemaDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();

		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID(), s -> {
			s.setName(s.getUuid());
			s.setCreated(user());
		});
		container.generateBucketId();
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			CoreTestUtils.fillSchemaVersion(v, container, schemaName, "1.0", creator.create(persistentFieldName), creator.create(
						removedFieldName));
			container.setLatestVersion(v);
		});
		schemaDao.mergeIntoPersisted(container);

		// create version 2 of the schema (with one field removed)
		HibSchemaVersion versionB = CoreTestUtils.createSchemaVersion(container, schemaName, "2.0", creator.create(persistentFieldName));

		// link the schemas with the change in between
		HibRemoveFieldChange change = (HibRemoveFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);
		versionA.setNextChange(change);

		// create a node based on the old schema
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
		HibUser user = user();
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		Tx.get().commit();
		HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, persistentFieldName);
		dataProvider.set(englishContainer, removedFieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);

		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());

		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(tx.contentDao().getFieldContainer(node, "en")).as("Migrated field container").isOf(versionB);
		assertThat(fetcher.fetch(tx.contentDao().getFieldContainer(node, "en"), persistentFieldName))
			.as("Field '" + persistentFieldName + "'").isNotNull();
		assertThat(fetcher.fetch(tx.contentDao().getFieldContainer(node, "en"), removedFieldName)).as("Field '" + removedFieldName + "'")
			.isNull();
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
	private void removeMicroschemaField(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher) throws InterruptedException,
		ExecutionException, TimeoutException {
		NodeDao nodeDao = tx.nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();

		String removedFieldName = "toremove";
		String persistentFieldName = "persistent";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		HibMicroschema container = microschemaDao.createPersisted(null, m -> {
			m.setName(microschemaName);
			m.setCreated(user());	
		});
		container.generateBucketId();
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", creator.create(persistentFieldName),
			creator.create(removedFieldName));

		// create version 2 of the microschema (with one field removed)
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", creator.create(persistentFieldName));

		// link the microschemas with the change in between
		HibRemoveFieldChange change = (HibRemoveFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		change.setFieldName(removedFieldName);
		change.setPreviousContainerVersion(versionA);
		change.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);
		versionA.setNextChange(change);

		// create a micronode based on the old microschema
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
		Tx.get().commit();
		HibMicronodeField micronodeField = createMicronodefield(tx, node, micronodeFieldName, versionA, dataProvider, persistentFieldName,
			removedFieldName);
		HibNodeFieldContainer oldContainer = tx.contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the node
		branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10, TimeUnit.SECONDS);

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = tx.contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
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
				renameMicroschemaField(tx, creator, dataProvider, fetcher, asserter);
			} else {
				renameSchemaField(tx, creator, dataProvider, fetcher, asserter);
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
	private void renameSchemaField(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
		throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = tx.nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		PersistingSchemaDao schemaDao = ((PersistingSchemaDao) tx.schemaDao());

		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID(), s -> {
			s.setName(s.getUuid());
			s.setCreated(user());
		});
		container.generateBucketId();
		schemaDao.mergeIntoPersisted(container);
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			CoreTestUtils.fillSchemaVersion(v, container, schemaName, "1.0", creator.create(oldFieldName));
			container.setLatestVersion(v);
		});

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		HibSchemaVersion versionB = CoreTestUtils.createSchemaVersion(container, schemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibAddFieldChange addFieldChange = (HibAddFieldChange) schemaDao.createPersistedChange(versionB, SchemaChangeOperation.ADDFIELD);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());

		HibRemoveFieldChange removeFieldChange = (HibRemoveFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);
		versionB.setNextChange(addFieldChange);

		// create a node based on the old schema
		HibUser user = user();
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionA, batch);
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, oldFieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionB, batch);
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);

		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(tx.contentDao().getFieldContainer(node, "en")).as("Migrated field container").isOf(versionB);
		assertThat(fetcher.fetch(tx.contentDao().getFieldContainer(node, "en"), oldFieldName)).as("Field '" + oldFieldName + "'").isNull();
		asserter.assertThat(tx.contentDao().getFieldContainer(node, "en"), newFieldName);
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
	private void renameMicroschemaField(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, DataAsserter asserter)
		throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = tx.nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();
		String oldFieldName = "oldname";
		String newFieldName = "newname";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		HibMicroschema container = microschemaDao.createPersisted(null, m -> {
			m.setName(microschemaName);
			m.setCreated(user());	
		});
		container.generateBucketId();
		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", creator.create(oldFieldName));

		// create version 2 of the microschema (with the field renamed)
		FieldSchema newField = creator.create(newFieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the microschemas with the changes in between
		HibAddFieldChange addFieldChange = (HibAddFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.ADDFIELD);
		addFieldChange.setFieldName(newFieldName);
		addFieldChange.setType(newField.getType());

		HibRemoveFieldChange removeFieldChange = (HibRemoveFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
		removeFieldChange.setFieldName(oldFieldName);

		addFieldChange.setPreviousContainerVersion(versionA);
		addFieldChange.setNextChange(removeFieldChange);
		removeFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
		HibMicronodeField micronodeField = createMicronodefield(tx, node, micronodeFieldName, versionA, dataProvider, oldFieldName);
		HibNodeFieldContainer oldContainer = tx.contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the micronode
		HibJob job = branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);
		if (job != null) {
			triggerAndWaitForJob(job.getUuid());
		} else {
			MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
			context.setBranch(project().getLatestBranch());
			context.setFromVersion(versionA);
			context.setToVersion(versionB);
			context.setStatus(DummyMigrationStatus.get());
			micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
				TimeUnit.SECONDS);
		}

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = tx.contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
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
		String schemaName = "schema_" + System.currentTimeMillis();

		AtomicReference<String> nodeUuid = new AtomicReference<>();
		AtomicReference<String> schemaUuid = new AtomicReference<>();
		AtomicReference<String> versionAUuid = new AtomicReference<>();
		AtomicReference<String> versionBUuid = new AtomicReference<>();
		AtomicReference<FieldSchema> oldFieldSchema = new AtomicReference<>();
		AtomicReference<FieldSchema> newFieldSchema = new AtomicReference<>();

		tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
			PersistingSchemaDao schemaDao = (PersistingSchemaDao) tx.schemaDao();


			// create version 1 of the schema
			oldFieldSchema.set(oldField.create(fieldName));
			HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID(), s -> {
				s.setName(schemaName);
				s.setCreated(user());
			});
			container.generateBucketId();
			HibSchemaVersion versionA = createSchemaVersion(tx, container, v -> {
				CoreTestUtils.fillSchemaVersion(v, container, schemaName, "1.0", oldFieldSchema.get());
				container.setLatestVersion(v);
			});
			versionAUuid.set(versionA.getUuid());
			schemaDao.mergeIntoPersisted(container);
			schemaUuid.set(container.getUuid());

			// create version 2 of the schema (with the field modified)
			newFieldSchema.set(newField.create(fieldName));
			HibSchemaVersion versionB = CoreTestUtils.createSchemaVersion(container, schemaName, "2.0", newFieldSchema.get());
			versionBUuid.set(versionB.getUuid());

			// link the schemas with the change in between
			HibFieldTypeChange change = (HibFieldTypeChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.CHANGEFIELDTYPE);
			change.setFieldName(fieldName);
			change.setType(newFieldSchema.get().getType());
			if (newFieldSchema.get() instanceof ListFieldSchema) {
				change.setListType(((ListFieldSchema) newFieldSchema.get()).getListType());
			}
			change.setPreviousContainerVersion(versionA);
			change.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);
			versionA.setNextChange(change);

			// create a node based on the old schema
			EventQueueBatch batch = createBatch();
			branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
			HibUser user = user();
			String english = english();
			HibNode parentNode = folder("2015");
			HibNode node = nodeDao.create(parentNode, user, versionA, project());
			nodeUuid.set(node.getUuid());
		});

		AtomicReference<HibNodeFieldContainer> englishContainer = new AtomicReference<>();
		tx(tx -> {
			HibUser user = user();
			String english = english();
			HibNode node = tx.nodeDao().findByUuidGlobal(nodeUuid.get());

			HibSchema schema = tx.schemaDao().findByUuid(schemaUuid.get());
			HibSchemaVersion versionA = tx.schemaDao().findVersionByUuid(schema, versionAUuid.get());
			HibSchemaVersion versionB = tx.schemaDao().findVersionByUuid(schema, versionBUuid.get());

			englishContainer.set(tx.contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
					user));
			dataProvider.set(englishContainer.get(), fieldName);
			assertThat(englishContainer.get()).isOf(versionA).hasVersion("0.1");

			if (dataProvider == FieldTestHelper.NOOP) {
				assertThat(oldFieldFetcher.fetch(englishContainer.get(), fieldName)).as(OLDFIELD).isNull();
			} else {
				assertThat(oldFieldFetcher.fetch(englishContainer.get(), fieldName)).as(OLDFIELD).isNotNull();
			}

			// migrate the node
			EventQueueBatch batch = createBatch();
			tx.branchDao().assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
		});

		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setHttpServerConfig(tx(tx -> { return tx.data().options().getHttpServerOptions();}));
		context.setProject(tx(() -> project()));
		context.setBranch(tx(() -> project().getLatestBranch()));
		context.setFromVersion(tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			return schemaDao.findVersionByUuid(schemaDao.findByUuid(schemaUuid.get()), versionAUuid.get());
		}));
		context.setToVersion(tx(tx -> {
			SchemaDao schemaDao = tx.schemaDao();
			return schemaDao.findVersionByUuid(schemaDao.findByUuid(schemaUuid.get()), versionBUuid.get());
		}));
		context.setStatus(DummyMigrationStatus.get());

		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		tx(tx -> {
			HibNode node = tx.nodeDao().findByUuidGlobal(nodeUuid.get());
			HibSchema schema = tx.schemaDao().findByUuid(schemaUuid.get());
			HibSchemaVersion versionA = tx.schemaDao().findVersionByUuid(schema, versionAUuid.get());
			HibSchemaVersion versionB = tx.schemaDao().findVersionByUuid(schema, versionBUuid.get());

			// old container must not be changed
			assertThat(englishContainer.get()).isOf(versionA).hasVersion("0.1");
			// assert that migration worked
			HibNodeFieldContainer migratedContainer = tx.contentDao().getFieldContainer(node, "en");
			assertThat(migratedContainer).isOf(versionB).hasVersion("0.2");
			assertThat(node).as("Migrated Node").isOf(schema).hasTranslation("en");

			if (!StringUtils.equals(oldFieldSchema.get().getType(), newFieldSchema.get().getType())) {
				assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
			}
			if ((oldFieldSchema.get() instanceof ListFieldSchema) && (newFieldSchema.get() instanceof ListFieldSchema) && !StringUtils.equals(
					((ListFieldSchema) oldFieldSchema.get()).getListType(), ((ListFieldSchema) newFieldSchema.get()).getListType())) {
				assertThat(oldFieldFetcher.fetch(migratedContainer, fieldName)).as(OLDFIELD).isNull();
			}
			asserter.assertThat(migratedContainer, fieldName);
		});
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

		AtomicReference<String> nodeUuid = new AtomicReference<>();
		AtomicReference<String> microschemaUuid = new AtomicReference<>();
		AtomicReference<String> versionAUuid = new AtomicReference<>();
		AtomicReference<String> versionBUuid = new AtomicReference<>();
		AtomicReference<FieldSchema> oldFieldSchema = new AtomicReference<>();
		AtomicReference<FieldSchema> newFieldSchema = new AtomicReference<>();

		tx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();

			// create version 1 of the microschema
			oldFieldSchema.set(oldField.create(fieldName));
			HibMicroschema container = microschemaDao.createPersisted(null, m -> {
				m.setName(microschemaName);
				m.setCreated(user());
			});
			microschemaUuid.set(container.getUuid());
			HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldFieldSchema.get());
			versionAUuid.set(versionA.getUuid());

			// create version 2 of the microschema (with the field modified)
			newFieldSchema.set(newField.create(fieldName));
			HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newFieldSchema.get());
			versionBUuid.set(versionB.getUuid());

			// link the schemas with the change in between
			HibFieldTypeChange change = (HibFieldTypeChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.CHANGEFIELDTYPE);
			change.setFieldName(fieldName);
			change.setType(newFieldSchema.get().getType());
			if (newFieldSchema.get() instanceof ListFieldSchema) {
				change.setListType(((ListFieldSchema) newFieldSchema.get()).getListType());
			}
			change.setPreviousContainerVersion(versionA);
			change.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);
			versionA.setNextChange(change);

			microschemaDao.assign(container, project(), user(), createBatch());
			HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
			nodeUuid.set(node.getUuid());
		});

		AtomicReference<VersionNumber> oldVersion = new AtomicReference<>();
		AtomicReference<HibNodeFieldContainer> oldContainer = new AtomicReference<>();
		tx(tx -> {
			HibNode node = tx.nodeDao().findByUuidGlobal(nodeUuid.get());
			HibMicroschema microschema = tx.microschemaDao().findByUuid(microschemaUuid.get());
			HibMicroschemaVersion versionA = tx.microschemaDao().findVersionByUuid(microschema, versionAUuid.get());

			// create a node based on the old schema
			HibMicronodeField micronodeField = createMicronodefield(tx, node, micronodeFieldName, versionA, dataProvider, fieldName);
			oldContainer.set(tx.contentDao().getFieldContainer(node, "en"));
			oldVersion.set(oldContainer.get().getVersion());

			if (dataProvider == FieldTestHelper.NOOP) {
				assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
			} else {
				assertThat(oldFieldFetcher.fetch(micronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNotNull();
			}
		});

		// migrate the micronode
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(tx(() -> project().getLatestBranch()));
		context.setFromVersion(tx(tx -> {
			MicroschemaDao microschemaDao = tx.microschemaDao();
			return microschemaDao.findVersionByUuid(microschemaDao.findByUuid(microschemaUuid.get()), versionAUuid.get());
		}));
		context.setToVersion(tx(tx -> {
			MicroschemaDao microschemaDao = tx.microschemaDao();
			return microschemaDao.findVersionByUuid(microschemaDao.findByUuid(microschemaUuid.get()), versionBUuid.get());
		}));
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10, TimeUnit.SECONDS);

		tx(tx -> {
			HibNode node = tx.nodeDao().findByUuidGlobal(nodeUuid.get());
			HibMicroschema microschema = tx.microschemaDao().findByUuid(microschemaUuid.get());
			HibMicroschemaVersion versionA = tx.microschemaDao().findVersionByUuid(microschema, versionAUuid.get());
			HibMicroschemaVersion versionB = tx.microschemaDao().findVersionByUuid(microschema, versionBUuid.get());

			// old container must be untouched
			HibMicronodeField micronodeField = oldContainer.get().getMicronode(micronodeFieldName);
			assertThat(oldContainer.get()).as("Old container").hasVersion(oldVersion.get().toString());
			assertThat(micronodeField.getMicronode()).as("Old micronode").isOf(versionA);

			// assert that migration worked
			HibNodeFieldContainer newContainer = tx.contentDao().getFieldContainer(node, "en");
			assertThat(newContainer).as("New container").hasVersion(oldVersion.get().nextDraft().toString());
			HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
			assertThat(newMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);

			if (!StringUtils.equals(oldFieldSchema.get().getType(), newFieldSchema.get().getType())) {
				assertThat(oldFieldFetcher.fetch(newMicronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
			}
			if ((oldFieldSchema.get() instanceof ListFieldSchema) && (newFieldSchema.get() instanceof ListFieldSchema) && !StringUtils.equals(
					((ListFieldSchema) oldFieldSchema.get()).getListType(), ((ListFieldSchema) newFieldSchema.get()).getListType())) {
				assertThat(oldFieldFetcher.fetch(newMicronodeField.getMicronode(), fieldName)).as(OLDFIELD).isNull();
			}
			asserter.assertThat(newMicronodeField.getMicronode(), fieldName);
		});
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
				customMicroschemaMigrationScript(tx, creator, dataProvider, fetcher, migrationScript, asserter);
			} else {
				customSchemaMigrationScript(tx, creator, dataProvider, fetcher, migrationScript, asserter);
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
	private void customSchemaMigrationScript(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		NodeDao nodeDao = tx.nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) tx.schemaDao();

		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID(), s -> {
			s.setName(UUIDUtil.randomUUID());
			s.setCreated(user());
		});
		container.generateBucketId();
		schemaDao.mergeIntoPersisted(container);
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			CoreTestUtils.fillSchemaVersion(v, container, schemaName, "1.0", oldField);
			container.setLatestVersion(v);
		});

		// Create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		HibSchemaVersion versionB = CoreTestUtils.createSchemaVersion(container, schemaName, "2.0", newField);

		// Link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
		HibUser user = user();
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);
		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());

		nodeMigrationHandler.migrateNodes(context).blockingAwait();

		// assert that migration worked
		assertThat(node).as("Migrated Node").isOf(container).hasTranslation("en");
		assertThat(tx.contentDao().getFieldContainer(node, "en")).as("Migrated field container").isOf(versionB);
		asserter.assertThat(tx.contentDao().getFieldContainer(node, "en"), fieldName);
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
	private void customMicroschemaMigrationScript(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, FieldFetcher fetcher, String migrationScript,
		DataAsserter asserter) throws InterruptedException, ExecutionException, TimeoutException {
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		NodeDao nodeDao = tx.nodeDao();

		String fieldName = "migratedField";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		HibMicroschema container = microschemaDao.createPersisted(null, m -> {
			m.setName(UUIDUtil.randomUUID());
			m.setCreated(user());
		});
		container.generateBucketId();

		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldField);

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		microschemaDao.assign(container, project(), user(), createBatch());
		HibNode node = nodeDao.create(folder("2015"), user(), schemaContainer("content").getLatestVersion(), project());
		HibMicronodeField micronodeField = createMicronodefield(tx, node, micronodeFieldName, versionA, dataProvider, fieldName);
		HibNodeFieldContainer oldContainer = tx.contentDao().getFieldContainer(node, "en");
		VersionNumber oldVersion = oldContainer.getVersion();

		// migrate the micronode
		branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);
		
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
			TimeUnit.SECONDS);

		// old container must be unchanged
		assertThat(oldContainer).as("Old container").hasVersion(oldVersion.toString());
		assertThat(micronodeField.getMicronode()).as("Old Micronode").isOf(versionA);

		// assert that migration worked
		HibNodeFieldContainer newContainer = tx.contentDao().getFieldContainer(node, "en");
		assertThat(newContainer).as("New container").hasVersion(oldVersion.nextDraft().toString());
		HibMicronodeField newMicronodeField = newContainer.getMicronode(micronodeFieldName);
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
					invalidMicroschemaMigrationScript(tx, creator, dataProvider, script);
				} else {
					invalidSchemaMigrationScript(tx, creator, dataProvider, script);
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
	private void invalidSchemaMigrationScript(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, String script) throws InterruptedException,
		ExecutionException, TimeoutException {
		NodeDao nodeDao = tx.nodeDao();
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		PersistingSchemaDao schemaDao = (PersistingSchemaDao) tx.schemaDao();

		String fieldName = "migratedField";
		String schemaName = "migratedSchema";

		// create version 1 of the schema
		FieldSchema oldField = creator.create(fieldName);
		HibSchema container = schemaDao.createPersisted(null, s -> {
			s.setName(UUIDUtil.randomUUID());
			s.setCreated(user());
		});

		schemaDao.mergeIntoPersisted(container);
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			CoreTestUtils.fillSchemaVersion(v, container, schemaName, "1.0", oldField);
			container.setLatestVersion(v);
		});

		// create version 2 of the schema (with the field renamed)
		FieldSchema newField = creator.create(fieldName);
		HibSchemaVersion versionB = CoreTestUtils.createSchemaVersion(container, schemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a node based on the old schema
		HibUser user = user();
		EventQueueBatch batch = createBatch();
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionA, batch);
		String english = english();
		HibNode parentNode = folder("2015");
		HibNode node = nodeDao.create(parentNode, user, versionA, project());
		HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user);
		dataProvider.set(englishContainer, fieldName);

		// migrate the node
		branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionB, batch);
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);
		NodeMigrationActionContextImpl context = new NodeMigrationActionContextImpl();
		context.setProject(project());
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		nodeMigrationHandler.migrateNodes(context).blockingAwait();
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
	private void invalidMicroschemaMigrationScript(Tx tx, FieldSchemaCreator creator, DataProvider dataProvider, String script) throws InterruptedException,
		ExecutionException, TimeoutException {
		String fieldName = "migratedField";
		String microschemaName = UUIDUtil.randomUUID();
		String micronodeFieldName = "micronodefield";
		PersistingBranchDao branchDao = (PersistingBranchDao) tx.branchDao();
		PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();

		// create version 1 of the microschema
		FieldSchema oldField = creator.create(fieldName);
		HibMicroschema container = microschemaDao.createPersisted(null, m -> {
			m.setName(microschemaName);
			m.setCreated(user());
		});

		HibMicroschemaVersion versionA = createMicroschemaVersion(container, microschemaName, "1.0", oldField);

		// create version 2 of the microschema
		FieldSchema newField = creator.create(fieldName);
		HibMicroschemaVersion versionB = createMicroschemaVersion(container, microschemaName, "2.0", newField);

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(fieldName);

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextVersion(versionB);

		// create a micronode based on the old schema
		createMicronodefield(tx, folder("2015"), micronodeFieldName, versionA, dataProvider, fieldName);

		// migrate the node
		branchDao.assignMicroschemaVersion(project().getLatestBranch(), user(), versionB, createBatch());
		CommonTx.get().commit();
		CommonTx.get().data().maybeGetEventQueueBatch().ifPresent(EventQueueBatch::dispatch);
		MicronodeMigrationContextImpl context = new MicronodeMigrationContextImpl();
		context.setBranch(project().getLatestBranch());
		context.setFromVersion(versionA);
		context.setToVersion(versionB);
		context.setStatus(DummyMigrationStatus.get());
		micronodeMigrationHandler.migrateMicronodes(context).blockingAwait(10,
			TimeUnit.SECONDS);
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
	protected HibMicroschemaVersion createMicroschemaVersion(HibMicroschema container, String name, String version,
		FieldSchema... fields) {
		MicroschemaVersionModel schema = new MicroschemaModelImpl();
		schema.setName(name);
		schema.setVersion(version);
		for (FieldSchema field : fields) {
			schema.addField(field);
		}
		HibMicroschemaVersion mversion = CommonTx.get().microschemaDao().createPersistedVersion(container, containerVersion -> {
			containerVersion.setSchema(schema);
			containerVersion.setName(name);
			containerVersion.setSchemaContainer(container);
			container.setLatestVersion(containerVersion);
		});
		Tx.get().commit();
		return mversion;
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
	protected HibMicronodeField createMicronodefield(Tx tx, HibNode node, String micronodeFieldName, HibMicroschemaVersion schemaVersion,
		DataProvider dataProvider, String... fieldNames) {
		String english = english();

		HibSchemaVersion latestVersion = node.getSchemaContainer().getLatestVersion();

		// Add a micronode field to the schema of the node.
		SchemaVersionModel schema = latestVersion.getSchema();
		if (schema.getField(micronodeFieldName) == null) {
			schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
		}
		schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(schemaVersion.getName());
		latestVersion.setSchema(schema);

		HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, english, node.getProject().getLatestBranch(),
			user());
		actions().updateSchemaVersion(englishContainer.getSchemaContainerVersion());
		HibMicronodeField micronodeField = englishContainer.createMicronode(micronodeFieldName, schemaVersion);
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
