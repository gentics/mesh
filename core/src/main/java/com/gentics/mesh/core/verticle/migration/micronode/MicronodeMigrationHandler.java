package com.gentics.mesh.core.verticle.migration.micronode;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
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
import com.gentics.mesh.core.verticle.migration.NodeMigrationStatus;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.Tuple;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Completable;
import rx.exceptions.CompositeException;

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
	 * @param project
	 *            project
	 * @param release
	 *            release
	 * @param fromVersion
	 *            microschema container version to start from
	 * @param toVersion
	 *            microschema container version to end with
	 * @param statusMBean
	 *            JMX Status bean
	 * @return Completable which will be invoked once the migration has completed
	 */
	public Completable migrateMicronodes(Project project, Release release, MicroschemaContainerVersion fromVersion,
			MicroschemaContainerVersion toVersion, NodeMigrationStatus statusMBean) {
		String releaseUuid = db.tx(release::getUuid);

		// get the containers, that need to be transformed
		List<? extends NodeGraphFieldContainer> fieldContainers = db.tx(() -> fromVersion.getFieldContainers(release.getUuid()));

		// no field containers, migration is done
		if (fieldContainers.isEmpty()) {
			return Completable.complete();
		}

		if (statusMBean != null) {
			statusMBean.setTotalNodes(fieldContainers.size());
		}

		// collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (Tx tx = db.tx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Completable.error(e);
		}

		NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
		ac.setProject(project);
		ac.setRelease(release);

		List<Completable> batches = new ArrayList<>();
		List<Exception> errorsDetected = new ArrayList<>();

		for (NodeGraphFieldContainer container : fieldContainers) {
			SearchQueueBatch batch = db.tx(() -> {
				SearchQueueBatch sqb = searchQueue.create();
				try {
					Node node = container.getParentNode();
					String languageTag = container.getLanguage().getLanguageTag();
					ac.getNodeParameters().setLanguages(languageTag);
					ac.getVersioningParameters().setVersion("draft");

					boolean publish = false;
					if (container.isPublished(releaseUuid)) {
						publish = true;
					} else {
						// check whether there is another published version
						NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, releaseUuid, PUBLISHED);
						if (oldPublished != null) {
							ac.getVersioningParameters().setVersion("published");

							// clone the field container
							NodeGraphFieldContainer migrated = node.createGraphFieldContainer(oldPublished.getLanguage(), release,
									oldPublished.getEditor(), oldPublished);

							migrated.setVersion(oldPublished.getVersion().nextPublished());
							node.setPublished(migrated, releaseUuid);

							// migrate
							migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields, migrationScripts);
							sqb.store(migrated, releaseUuid, PUBLISHED, false);
							ac.getVersioningParameters().setVersion("draft");
						}
					}

					NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(),
							container);
					if (publish) {
						migrated.setVersion(container.getVersion().nextPublished());
						node.setPublished(migrated, releaseUuid);
					}

					// migrate
					migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields, migrationScripts);

					sqb.store(node, releaseUuid, DRAFT, false);
					if (publish) {
						sqb.store(node, releaseUuid, PUBLISHED, false);
					}
					return sqb;
				} catch (Exception e1) {
					log.error("Error while handling container {" + container.getUuid() + "} during schema migration.", e1);
					errorsDetected.add(e1);
					return null;
				}
			});

			// Process the search queue batch in order to update the search index
			if (batch != null) {
				batches.add(batch.processAsync());
			}

			if (statusMBean != null) {
				statusMBean.incNodesDone();
			}
		}

		Completable result = Completable.complete();
		if (!errorsDetected.isEmpty()) {
			result = Completable.error(new CompositeException(errorsDetected));
		}

		return Completable.merge(batches).andThen(result);
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
