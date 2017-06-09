package com.gentics.mesh.core.data.node.handler;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.INITIAL;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.script.ScriptEngine;

import com.gentics.ferma.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.FieldTypeChangeImpl;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.node.BinaryFieldHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationStatus;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import rx.Completable;
import rx.exceptions.CompositeException;

/**
 * Handler for node migrations after schema updates
 */
@SuppressWarnings("restriction")
public class NodeMigrationHandler extends AbstractHandler {

	private static final Logger log = LoggerFactory.getLogger(NodeMigrationHandler.class);

	private BinaryFieldHandler nodeFieldAPIHandler;

	private Database db;

	private SearchQueue searchQueue;

	@Inject
	public NodeMigrationHandler(BinaryFieldHandler nodeFieldAPIHandler, Database db, SearchQueue searchQueue) {
		this.db = db;
		this.nodeFieldAPIHandler = nodeFieldAPIHandler;
		this.searchQueue = searchQueue;
	}

	/**
	 * Script engine factory.
	 */
	private NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

	/**
	 * Migrate all nodes of a release referencing the given schema container to the latest version of the schema.
	 *
	 * @param project
	 *            Specific project to handle
	 * @param release
	 *            Specific release to handle
	 * @param fromVersion
	 *            Old container version
	 * @param toVersion
	 *            New container version
	 * @param statusMBean
	 *            status MBean
	 */
	public Completable migrateNodes(Project project, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion,
			NodeMigrationStatus statusMBean) {
		String releaseUuid = db.tx(release::getUuid);

		// get the nodes, that need to be transformed
		List<? extends NodeGraphFieldContainer> fieldContainers = db.tx(() -> fromVersion.getFieldContainers(releaseUuid));

		// no field containers -> no nodes, migration is done
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

		SchemaModel newSchema = toVersion.getSchema();
		List<Completable> batches = new ArrayList<>();
		// SearchQueueBatch indexCreatingBatch = searchQueue.create();
		// indexCreatingBatch.createNodeIndex(project.getUuid(), releaseUuid, toVersion.getUuid(), DRAFT, toVersion.getSchema());
		// indexCreatingBatch.createNodeIndex(project.getUuid(), releaseUuid, toVersion.getUuid(), PUBLISHED, toVersion.getSchema());

		List<Exception> errorsDetected = new ArrayList<>();

		// Iterate over all containers and invoke a migration for each one
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
							NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);
							restModel.getSchema().setVersion(newSchema.getVersion());

							NodeGraphFieldContainer migrated = node.createGraphFieldContainer(oldPublished.getLanguage(), release,
									oldPublished.getEditor(), oldPublished);
							migrated.setVersion(oldPublished.getVersion().nextPublished());
							node.setPublished(migrated, releaseUuid);
							migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);
							sqb.store(migrated, releaseUuid, PUBLISHED, false);

							ac.getVersioningParameters().setVersion("draft");
						}
					}

					NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag);

					// Update the schema version. Otherwise deserialisation of the JSON will fail later on.
					restModel.getSchema().setVersion(newSchema.getVersion());

					// Invoke the migration
					NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release, container.getEditor(),
							container);
					if (publish) {
						migrated.setVersion(container.getVersion().nextPublished());
						node.setPublished(migrated, releaseUuid);
					}
					migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

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
	 * Migrate all nodes from one release to the other
	 * 
	 * @param newRelease
	 *            new release
	 * @return Completable which will be invoked once the migration has completed
	 */
	public Completable migrateNodes(Release newRelease) {
		Release oldRelease = db.tx(() -> {
			if (newRelease.isMigrated()) {
				throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} is already migrated");
			}

			Release old = newRelease.getPreviousRelease();
			if (old == null) {
				throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} does not have previous release");
			}

			if (!old.isMigrated()) {
				throw error(BAD_REQUEST, "Cannot migrate nodes to release {" + newRelease.getName() + "}, because previous release {" + old.getName()
						+ "} is not fully migrated yet.");
			}

			return old;
		});

		String oldReleaseUuid = db.tx(() -> oldRelease.getUuid());
		String newReleaseUuid = db.tx(() -> newRelease.getUuid());
		List<? extends Node> nodes = db.tx(() -> oldRelease.getRoot().getProject().getNodeRoot().findAll());
		List<Completable> batches = new ArrayList<>();
		for (Node node : nodes) {
			SearchQueueBatch sqb = db.tx(() -> {
				if (!node.getGraphFieldContainers(newRelease, INITIAL).isEmpty()) {
					return null;
				}
				node.getGraphFieldContainers(oldRelease, DRAFT).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl initialEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					initialEdge.setType(INITIAL);
					initialEdge.setReleaseUuid(newReleaseUuid);

					GraphFieldContainerEdgeImpl draftEdge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					draftEdge.setType(DRAFT);
					draftEdge.setReleaseUuid(newReleaseUuid);
				});
				SearchQueueBatch batch = searchQueue.create();
				batch.store(node, newReleaseUuid, DRAFT, false);

				node.getGraphFieldContainers(oldRelease, PUBLISHED).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl edge = node.addFramedEdge(HAS_FIELD_CONTAINER, container, GraphFieldContainerEdgeImpl.class);
					edge.setLanguageTag(container.getLanguage().getLanguageTag());
					edge.setType(PUBLISHED);
					edge.setReleaseUuid(newReleaseUuid);
				});
				batch.store(node, newReleaseUuid, PUBLISHED, false);

				Node parent = node.getParentNode(oldReleaseUuid);
				if (parent != null) {
					node.setParentNode(newReleaseUuid, parent);
				}

				// migrate tags
				node.getTags(oldRelease).forEach(tag -> node.addTag(tag, newRelease));
				return batch;
			});
			batches.add(sqb.processAsync());
		}

		db.tx(() -> {
			newRelease.setMigrated(true);
			return null;
		});

		return Completable.merge(batches);
	}

	/**
	 * Migrate the given container. This will also set the new version to the container
	 * 
	 * @param ac
	 *            context
	 * @param container
	 *            container to migrate
	 * @param restModel
	 *            rest model of the container
	 * @param newVersion
	 *            new schema version
	 * @param touchedFields
	 *            set of touched fields
	 * @param migrationScripts
	 *            list of migration scripts
	 * @param clazz
	 * @throws Exception
	 */
	protected <T extends FieldContainer> void migrate(NodeMigrationActionContextImpl ac, GraphFieldContainer container, RestModel restModel,
			GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> newVersion, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Class<T> clazz) throws Exception {
		// collect the files for all binary fields (keys are the sha512sums,
		// values are filepaths to the binary files)
		Map<String, String> filePaths = container.getFields().stream().filter(f -> f instanceof BinaryGraphField).map(f -> (BinaryGraphField) f)
				.collect(Collectors.toMap(BinaryGraphField::getSHA512Sum, BinaryGraphField::getFilePath, (existingPath, newPath) -> existingPath));

		// remove all touched fields (if necessary, they will be readded later)
		container.getFields().stream().filter(f -> touchedFields.contains(f.getFieldKey())).forEach(f -> f.removeField(container));

		String nodeJson = JsonUtil.toJson(restModel);

		for (Tuple<String, List<Tuple<String, Object>>> scriptEntry : migrationScripts) {
			String script = scriptEntry.v1();
			List<Tuple<String, Object>> context = scriptEntry.v2();
			ScriptEngine engine = factory.getScriptEngine(new Sandbox());

			engine.put("node", nodeJson);
			engine.put("convert", new TypeConverter());
			if (context != null) {
				for (Tuple<String, Object> ctxEntry : context) {
					engine.put(ctxEntry.v1(), ctxEntry.v2());
				}
			}
			engine.eval(script);

			Object transformedNodeModel = engine.get("node");

			if (transformedNodeModel == null) {
				throw new Exception("Transformed node model not found after handling migration scripts");
			}

			nodeJson = transformedNodeModel.toString();
		}

		// transform the result back to the Rest Model
		T transformedRestModel = JsonUtil.readValue(nodeJson, clazz);

		container.setSchemaContainerVersion(newVersion);
		container.updateFieldsFromRest(ac, transformedRestModel.getFields());
		// create a map containing fieldnames (as keys) and
		// sha512sums of the supposedly stored binary contents
		// of all binary fields
		Map<String, String> existingBinaryFields = newVersion.getSchema().getFields().stream().filter(f -> "binary".equals(f.getType()))
				.map(f -> Tuple.tuple(f.getName(), transformedRestModel.getFields().getBinaryField(f.getName()))).filter(t -> t.v2() != null)
				.filter(t -> t.v2().getSha512sum() != null).collect(Collectors.toMap(t -> t.v1(), t -> t.v2().getSha512sum()));

		// check for every binary field in the migrated node,
		// whether the binary file is present, if not, copy it
		// from the old data
		existingBinaryFields.entrySet().stream().forEach(entry -> {
			String fieldName = entry.getKey();
			String sha512Sum = entry.getValue();

			BinaryGraphField binaryField = container.getBinary(fieldName);
			if (binaryField != null && !binaryField.getFile().exists() && filePaths.containsKey(sha512Sum)) {
				Buffer buffer = Mesh.vertx().fileSystem().readFileBlocking(filePaths.get(sha512Sum));
				nodeFieldAPIHandler.hashAndStoreBinaryFile(buffer, binaryField.getUuid(), binaryField.getSegmentedPath());
				binaryField.setSHA512Sum(sha512Sum);
			}
		});
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

	/**
	 * Collect the migration scripts and set of touched fields when migrating the given container into the next version
	 *
	 * @param fromVersion
	 *            Container which contains the expected migration changes
	 * @param migrationScripts
	 *            List of migration scripts (will be modified)
	 * @param touchedFields
	 *            Set of touched fields (will be modified)
	 * @throws IOException
	 */
	protected void prepareMigration(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> fromVersion,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Set<String> touchedFields) throws IOException {
		SchemaChange<?> change = fromVersion.getNextChange();
		while (change != null) {
			String migrationScript = change.getMigrationScript();
			if (migrationScript != null) {
				migrationScript = migrationScript + "\nnode = JSON.stringify(migrate(JSON.parse(node), fieldname, convert));";
				migrationScripts.add(Tuple.tuple(migrationScript, change.getMigrationScriptContext()));
			}

			// if either the type changes or the field is removed, the field is
			// "touched"
			if (change instanceof FieldTypeChangeImpl) {
				touchedFields.add(((FieldTypeChangeImpl) change).getFieldName());
			} else if (change instanceof RemoveFieldChange) {
				touchedFields.add(((RemoveFieldChange) change).getFieldName());
			}

			change = change.getNextChange();
		}
	}

	/**
	 * Sandbox classfilter that filters all classes
	 */
	protected static class Sandbox implements ClassFilter {
		@Override
		public boolean exposeToScripts(String className) {
			return false;
		}
	}
}
