package com.gentics.mesh.core.data.node.handler;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.script.ScriptEngine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.ContainerType;
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
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.core.verticle.node.NodeFieldAPIHandler;
import com.gentics.mesh.core.verticle.node.NodeMigrationStatus;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.rxjava.core.buffer.Buffer;
import jdk.nashorn.api.scripting.ClassFilter;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import rx.Completable;
import rx.Observable;

/**
 * Handler for node migrations after schema updates
 */
@SuppressWarnings("restriction")
@Component
public class NodeMigrationHandler extends AbstractHandler {

	@Autowired
	private NodeFieldAPIHandler nodeFieldAPIHandler;

	/**
	 * Script engine factory
	 */
	private NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

	/**
	 * Migrate all nodes of a release referencing the given schema container to the latest version of the schema
	 *
	 * @param project
	 * @param release
	 * @param fromVersion
	 * @param toVersion
	 * @param statusMBean
	 *            status MBean
	 */
	public Completable migrateNodes(Project project, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion,
			NodeMigrationStatus statusMBean) {
		String releaseUuid = db.noTrx(release::getUuid);

		// get the nodes, that need to be transformed
		List<? extends NodeGraphFieldContainer> fieldContainers = db
				.noTrx(() -> fromVersion.getFieldContainers(releaseUuid));


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
		try (NoTrx noTrx = db.noTrx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Completable.error(e);
		}
		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());

		NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
		ac.setProject(project);
		ac.setRelease(release);

		Schema newSchema = toVersion.getSchema();

		// Iterate over all containers and invoke a migration for each one
		for (NodeGraphFieldContainer container : fieldContainers) {
			Exception e = db.trx(() -> {
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
						NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, releaseUuid, ContainerType.PUBLISHED);
						if (oldPublished != null) {
							ac.getVersioningParameters().setVersion("published");
							NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag).toBlocking().value();
							restModel.getSchema().setVersion(newSchema.getVersion());

							NodeGraphFieldContainer migrated = node.createGraphFieldContainer(
									oldPublished.getLanguage(), release, oldPublished.getEditor(), oldPublished);
							migrated.setVersion(oldPublished.getVersion().nextPublished());
							node.setPublished(migrated, releaseUuid);
							migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

							migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, ContainerType.PUBLISHED);

							ac.getVersioningParameters().setVersion("draft");
						}
					}

					NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag).toBlocking().value();

					// Update the schema version. Otherwise deserialisation of the JSON will fail later on.
					restModel.getSchema().setVersion(newSchema.getVersion());

					// Invoke the migration
					NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release,
							container.getEditor(), container);
					if (publish) {
						migrated.setVersion(container.getVersion().nextPublished());
						node.setPublished(migrated, releaseUuid);
					}
					migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

					migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, ContainerType.DRAFT);
					if (publish) {
						migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, ContainerType.PUBLISHED);
					}

					return null;
				} catch (Exception e1) {
					return e1;
				}
			});

			if (e != null) {
				return Completable.error(e);
			}

			if (statusMBean != null) {
				statusMBean.incNodesDone();
			}
		}

		// Process the search queue batch in order to update the search index
		return batch.process();
	}

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 *
	 * @param project project
	 * @param release release
	 * @param fromVersion
	 *            microschema container version to start from
	 * @param toVersion
	 *            microschema container version to end with
	 * @param statusMBean
	 *            JMX Status bean
	 * @return
	 */
	public Observable<Void> migrateMicronodes(Project project, Release release, MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
			NodeMigrationStatus statusMBean) {
		String releaseUuid = db.noTrx(release::getUuid);

		// get the containers, that need to be transformed
		List<? extends NodeGraphFieldContainer> fieldContainers = db
				.noTrx(() -> fromVersion.getFieldContainers(release.getUuid()));

		// no field containers, migration is done
		if (fieldContainers.isEmpty()) {
			return Observable.just(null);
		}

		if (statusMBean != null) {
			statusMBean.setTotalNodes(fieldContainers.size());
		}

		// collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (NoTrx noTrx = db.noTrx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Observable.error(e);
		}

		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());

		NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
		ac.setProject(project);
		ac.setRelease(release);

		for (NodeGraphFieldContainer container : fieldContainers) {
			Exception e = db.trx(() -> {
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
						NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, releaseUuid, ContainerType.PUBLISHED);
						if (oldPublished != null) {
							ac.getVersioningParameters().setVersion("published");

							// clone the field container
							NodeGraphFieldContainer migrated = node.createGraphFieldContainer(
									oldPublished.getLanguage(), release, oldPublished.getEditor(), oldPublished);

							migrated.setVersion(oldPublished.getVersion().nextPublished());
							node.setPublished(migrated, releaseUuid);

							// migrate
							migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields,
									migrationScripts);

							migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, ContainerType.PUBLISHED);

							ac.getVersioningParameters().setVersion("draft");
						}
					}

					NodeGraphFieldContainer migrated = node.createGraphFieldContainer(container.getLanguage(), release,
							container.getEditor(), container);
					if (publish) {
						migrated.setVersion(container.getVersion().nextPublished());
						node.setPublished(migrated, releaseUuid);
					}

					// migrate
					migrateMicronodeFields(ac, migrated, fromVersion, toVersion, touchedFields,
							migrationScripts);

					migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, ContainerType.DRAFT);
					if (publish) {
						migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, ContainerType.PUBLISHED);
					}

					return null;
				} catch (Exception e1) {
					return e1;
				}
			});

			if (e != null) {
				return Observable.error(e);
			}

			if (statusMBean != null) {
				statusMBean.incNodesDone();
			}
		}

		return Observable.just(null);
	}

	/**
	 * Migrate all nodes from one release to the other
	 * @param newRelease new release
	 * @return
	 */
	public Completable migrateNodes(Release newRelease) {
		Release oldRelease = db.noTrx(() -> {
			if (newRelease.isMigrated()) {
				throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} is already migrated");
			}

			Release old = newRelease.getPreviousRelease();
			if (old == null) {
				throw error(BAD_REQUEST, "Release {" + newRelease.getName() + "} does not have previous release");
			}

			if (!old.isMigrated()) {
				throw error(BAD_REQUEST, "Cannot migrate nodes to release {" + newRelease.getName()
						+ "}, because previous release {" + old.getName() + "} is not fully migrated yet.");
			}

			return old;
		});

		String oldReleaseUuid = db.noTrx(() -> oldRelease.getUuid());
		String newReleaseUuid = db.noTrx(() -> newRelease.getUuid());
		List<? extends Node> nodes = db.noTrx(() -> oldRelease.getRoot().getProject().getNodeRoot().findAll());

		SearchQueue queue = BootstrapInitializer.getBoot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch(UUIDUtil.randomUUID());
		for (Node node : nodes) {
			db.trx(() -> {
				if (!node.getGraphFieldContainers(newRelease, ContainerType.INITIAL).isEmpty()) {
					return null;
				}
				node.getGraphFieldContainers(oldRelease, ContainerType.DRAFT).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl initialEdge = node.getImpl().addFramedEdge(HAS_FIELD_CONTAINER,
							container.getImpl(), GraphFieldContainerEdgeImpl.class);
					initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					initialEdge.setType(ContainerType.INITIAL);
					initialEdge.setReleaseUuid(newReleaseUuid);

					GraphFieldContainerEdgeImpl draftEdge = node.getImpl().addFramedEdge(HAS_FIELD_CONTAINER,
							container.getImpl(), GraphFieldContainerEdgeImpl.class);
					draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					draftEdge.setType(ContainerType.DRAFT);
					draftEdge.setReleaseUuid(newReleaseUuid);

					container.addIndexBatchEntry(batch, STORE_ACTION, newReleaseUuid, ContainerType.DRAFT);
				});

				node.getGraphFieldContainers(oldRelease, ContainerType.PUBLISHED).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl edge = node.getImpl().addFramedEdge(HAS_FIELD_CONTAINER,
							container.getImpl(), GraphFieldContainerEdgeImpl.class);
					edge.setLanguageTag(container.getLanguage().getLanguageTag());
					edge.setType(ContainerType.PUBLISHED);
					edge.setReleaseUuid(newReleaseUuid);

					container.addIndexBatchEntry(batch, STORE_ACTION, newReleaseUuid, ContainerType.PUBLISHED);
				});

				Node parent = node.getParentNode(oldReleaseUuid);
				if (parent != null) {
					node.setParentNode(newReleaseUuid, parent);
				}

				// migrate tags
				node.getTags(oldRelease).forEach(tag -> node.addTag(tag, newRelease));
				return null;
			});
		}

		db.trx(() -> {
			newRelease.setMigrated(true);
			return null;
		});

		// Process the search queue batch in order to update the search index
		return batch.process();
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
			GraphFieldSchemaContainerVersion<?, ?, ?, ?> newVersion, Set<String> touchedFields,
			List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts, Class<T> clazz) throws Exception {
		// collect the files for all binary fields (keys are the sha512sums,
		// values are filepaths to the binary files)
		Map<String, String> filePaths = container.getFields().stream().filter(f -> f instanceof BinaryGraphField)
				.map(f -> (BinaryGraphField) f)
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
				.collect(Collectors.toMap(t -> t.v1(), t -> t.v2().getSha512sum()));

		// check for every binary field in the migrated node,
		// whether the binary file is present, if not, copy it
		// from the old data
		existingBinaryFields.entrySet().stream().forEach(entry -> {
			String fieldName = entry.getKey();
			String sha512Sum = entry.getValue();

			BinaryGraphField binaryField = container.getBinary(fieldName);
			if (binaryField != null && !binaryField.getFile().exists() && filePaths.containsKey(sha512Sum)) {
				Buffer buffer = Buffer.newInstance(Mesh.vertx().fileSystem().readFileBlocking(filePaths.get(sha512Sum)));
				nodeFieldAPIHandler.hashAndStoreBinaryFile(buffer, binaryField.getUuid(), binaryField.getSegmentedPath()).toBlocking().last();
				binaryField.setSHA512Sum(sha512Sum);
			}
		});
	}

	/**
	 * Migrate all micronode fields from old schema version to new schema version
	 * @param ac action context
	 * @param container field container
	 * @param fromVersion old schema version
	 * @param toVersion new schema version
	 * @param touchedFields touched fields
	 * @param migrationScripts migration scripts
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
			MicronodeResponse restModel = micronode.transformToRestSync(ac, 0).toBlocking().value();
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
				if (newMicronode.getSchemaContainerVersion().getImpl().equals(fromVersion.getImpl())) {
					// transform to rest and migrate
					MicronodeResponse restModel = newMicronode.transformToRestSync(ac, 0).toBlocking().value();
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
	protected void prepareMigration(GraphFieldSchemaContainerVersion<?, ?, ?, ?> fromVersion,
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
