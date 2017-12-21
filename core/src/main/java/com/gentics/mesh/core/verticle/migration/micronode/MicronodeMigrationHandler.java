package com.gentics.mesh.core.verticle.migration.micronode;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.RUNNING;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.verticle.migration.AbstractMigrationHandler;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.VersionNumber;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Completable;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class MicronodeMigrationHandler extends AbstractMigrationHandler {

	private static final Logger log = LoggerFactory.getLogger(MicronodeMigrationHandler.class);

	@Inject
	public MicronodeMigrationHandler(Database db, SearchQueue searchQueue, BinaryFieldHandler binaryFieldHandler) {
		super(db, searchQueue, binaryFieldHandler);
	}

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 * 
	 * @param release
	 *            release
	 * @param fromVersion
	 *            microschema container version to start from
	 * @param toVersion
	 *            microschema container version to end with
	 * @param status
	 *            Migration status
	 * @return Completable which will be completed once the migration has completed
	 */
	public Completable migrateMicronodes(Release release, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
			MigrationStatusHandler status) {

		// Get the containers, that need to be transformed
		Iterator<? extends NodeGraphFieldContainer> fieldContainersIt = db.tx(() -> fromVersion.getDraftFieldContainers(release.getUuid()));

		// No field containers, migration is done
		if (!fieldContainersIt.hasNext()) {
			return Completable.complete();
		}

		// Collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (Tx tx = db.tx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Completable.error(e);
		}

		NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
		ac.setProject(release.getProject());
		ac.setRelease(release);

		if (status != null) {
			status.setStatus(RUNNING);
			status.commit();
		}

		// Iterate over all containers and invoke a migration for each one
		long count = 0;
		List<Exception> errorsDetected = new ArrayList<>();
		while (fieldContainersIt.hasNext()) {
			NodeGraphFieldContainer container = fieldContainersIt.next();
			migrateMicronodeContainer(ac, release, fromVersion, toVersion, container, touchedFields, migrationScripts, errorsDetected);

			if (status != null) {
				status.incCompleted();
			}
			if (count % 50 == 0) {
				log.info("Migrated micronode containers: " + count);
				if (status != null) {
					status.commit();
				}
			}
			count++;
		}
		log.info("Migration of " + count + " containers done..");
		log.info("Encountered {" + errorsDetected.size() + "} errors during micronode migration.");
		Completable result = Completable.complete();
		if (!errorsDetected.isEmpty()) {
			if (log.isDebugEnabled()) {
				for (Exception error : errorsDetected) {
					log.error("Encountered migration error.", error);
				}
			}
			result = Completable.error(new CompositeException(errorsDetected));
		}
		return result;
	}

	private void migrateMicronodeContainer(NodeMigrationActionContextImpl ac, Release release, MicroschemaContainerVersion fromVersion,
			MicroschemaContainerVersion toVersion, NodeGraphFieldContainer container, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, List<Exception> errorsDetected) {

		if (log.isDebugEnabled()) {
			log.debug("Migrating container {" + container.getUuid() + "}");
		}
		String releaseUuid = release.getUuid();

		// Run the actual migration in a dedicated transaction
		try {
			SearchQueueBatch batch = db.tx((tx) -> {
				SearchQueueBatch sqb = searchQueue.create();

				Node node = container.getParentNode();
				String languageTag = container.getLanguage().getLanguageTag();
				ac.getNodeParameters().setLanguages(languageTag);
				ac.getVersioningParameters().setVersion("draft");
				NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, releaseUuid, PUBLISHED);

				VersionNumber nextDraftVersion = null;
				// 1. Check whether there is any other published container which we need to handle separately
				if (oldPublished != null && !oldPublished.equals(container)) {
					nextDraftVersion = migratePublishedContainer(ac, sqb, release, node, container, fromVersion, toVersion, touchedFields,
							migrationScripts);
					nextDraftVersion = nextDraftVersion.nextDraft();
				}

				// 2. Migrate the draft container. This will also update the draft edge.
				migrateDraftContainer(ac, sqb, release, node, container, fromVersion, toVersion, touchedFields, migrationScripts, nextDraftVersion);

				return sqb;
			});
			// Process the search queue batch in order to update the search index
			if (batch != null) {
				batch.processSync();
			}

		} catch (Exception e1) {
			log.error("Error while handling container {" + container.getUuid() + "} during schema migration.", e1);
			errorsDetected.add(e1);
		}

	}

	/**
	 * Migrate the draft container. Internally we will also check whether the container is also the published container and handle this case correctly.
	 * 
	 * @param ac
	 * @param sqb
	 * @param release
	 * @param node
	 * @param container
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @param migrationScripts
	 * @param nextDraftVersion
	 * @throws Exception
	 */
	private void migrateDraftContainer(NodeMigrationActionContextImpl ac, SearchQueueBatch sqb, Release release, Node node,
			NodeGraphFieldContainer container, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
			Set<String> touchedFields, List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, VersionNumber nextDraftVersion)
			throws Exception {

		String releaseUuid = release.getUuid();
		ac.getVersioningParameters().setVersion("draft");

		boolean publish = container.isPublished(releaseUuid);

		// Clone the field container. This will also update the draft edge
		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(), container, true);
		if (publish) {
			migrated.setVersion(container.getVersion().nextPublished());
			// Ensure that the publish edge is also updated correctly
			node.setPublished(migrated, releaseUuid);
		} else {
			if (nextDraftVersion == null) {
				nextDraftVersion = container.getVersion().nextDraft();
			}
			migrated.setVersion(nextDraftVersion);
		}

		migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields, migrationScripts);

		// Ensure the search index is updated accordingly
		sqb.store(node, releaseUuid, DRAFT, false);
		if (publish) {
			sqb.store(node, releaseUuid, PUBLISHED, false);
		}
	}

	/**
	 * Migrate the published container.
	 * 
	 * @param ac
	 * @param sqb
	 *            Batch to be used to update the search index
	 * @param release
	 * @param node
	 *            Node of the container
	 * @param container
	 *            Container to be migrated
	 * @param fromVersion
	 * @param toVersion
	 * @param touchedFields
	 * @param migrationScripts
	 * @return Version of the new published container
	 * @throws Exception
	 */
	private VersionNumber migratePublishedContainer(NodeMigrationActionContextImpl ac, SearchQueueBatch sqb, Release release, Node node,
			NodeGraphFieldContainer container, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
			Set<String> touchedFields, List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts) throws Exception {

		String releaseUuid = release.getUuid();
		ac.getVersioningParameters().setVersion("published");

		NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(), container, false);
		migrated.setVersion(container.getVersion().nextPublished());
		node.setPublished(migrated, releaseUuid);

		migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields, migrationScripts);
		sqb.store(migrated, releaseUuid, PUBLISHED, false);
		return migrated.getVersion();

	}

	/**
	 * Migrate all micronode fields from old schema version to new schema version
	 * 
	 * @param ac
	 *            action context
	 * @param container
	 *            field container
	 * @param fromVersion
	 *            old schema version
	 * @param toVersion
	 *            new schema version
	 * @param touchedFields
	 *            touched fields
	 * @param migrationScripts
	 *            migration scripts
	 * @throws Exception
	 */
	protected void migrateMicronodeFields(NodeMigrationActionContextImpl ac, NodeGraphFieldContainer container,
			MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts) throws Exception {
		// iterate over all fields with micronodes to migrate
		for (MicronodeGraphField field : container.getMicronodeFields(fromVersion)) {
			// clone the field (this will clone the micronode)
			field = container.createMicronode(field.getFieldKey(), fromVersion);
			Micronode micronode = field.getMicronode();
			// transform to rest and migrate
			MicronodeResponse restModel = micronode.transformToRestSync(ac, 0);
			migrate(ac, micronode, restModel, toVersion, touchedFields, migrationScripts, MicronodeResponse.class);
		}

		// iterate over all micronode list fields to migrate
		for (MicronodeGraphFieldList field : container.getMicronodeListFields(fromVersion)) {
			MicronodeGraphFieldList oldListField = field;

			// clone the field (this will not clone the micronodes)
			field = container.createMicronodeFieldList(field.getFieldKey());

			// clone every micronode
			for (MicronodeGraphField oldField : oldListField.getList()) {
				Micronode oldMicronode = oldField.getMicronode();
				Micronode newMicronode = field.createMicronode();
				newMicronode.setSchemaContainerVersion(oldMicronode.getSchemaContainerVersion());
				newMicronode.clone(oldMicronode);

				// migrate the micronode, if it uses the fromVersion
				if (newMicronode.getSchemaContainerVersion().equals(fromVersion)) {
					// transform to rest and migrate
					MicronodeResponse restModel = newMicronode.transformToRestSync(ac, 0);
					migrate(ac, newMicronode, restModel, toVersion, touchedFields, migrationScripts, MicronodeResponse.class);
				}
			}
		}
	}

}
