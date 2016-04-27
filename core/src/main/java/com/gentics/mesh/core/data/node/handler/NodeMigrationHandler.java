package com.gentics.mesh.core.data.node.handler;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
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
	 * @return Observable which contains the search queue batch which needs to be processed
	 */
	public Observable<SearchQueueBatch> migrateNodes(Project project, Release release, SchemaContainerVersion fromVersion, SchemaContainerVersion toVersion,
			NodeMigrationStatus statusMBean) {
		String releaseUuid = db.noTrx(release::getUuid);

		// get the nodes, that need to be transformed
		List<? extends NodeGraphFieldContainer> fieldContainers = db
				.noTrx(() -> fromVersion.getFieldContainers(releaseUuid));


		// no field containers -> no nodes, migration is done
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

		Schema newSchema = toVersion.getSchema();

		// Iterate over all containers and invoke a migration for each one
		for (NodeGraphFieldContainer container : fieldContainers) {
			Exception e = db.trx(() -> {
				try {
					Node node = container.getParentNode();
					String languageTag = container.getLanguage().getLanguageTag();
					ac.setLanguageTags(Arrays.asList(languageTag));
					ac.setVersion("draft");

					boolean publish = false;
					if (container.isPublished(releaseUuid)) {
						publish = true;
					} else {
						// check whether there is another published version
						NodeGraphFieldContainer oldPublished = node.getGraphFieldContainer(languageTag, releaseUuid, Type.PUBLISHED);
						if (oldPublished != null) {
							ac.setVersion("published");
							NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag).toBlocking().last();
							restModel.getSchema().setVersion(newSchema.getVersion());

							NodeGraphFieldContainer migrated = node.createGraphFieldContainer(
									oldPublished.getLanguage(), release, oldPublished.getEditor(), oldPublished);
							migrated.setVersion(oldPublished.getVersion().nextPublished());
							node.setPublished(migrated, releaseUuid);
							migrate(ac, migrated, restModel, toVersion, touchedFields, migrationScripts, NodeUpdateRequest.class);

							migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, Type.PUBLISHED);

							ac.setVersion("draft");
						}
					}

					NodeResponse restModel = node.transformToRestSync(ac, 0, languageTag).toBlocking().last();

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

					migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, Type.DRAFT);
					if (publish) {
						migrated.addIndexBatchEntry(batch, STORE_ACTION, releaseUuid, Type.PUBLISHED);
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

		// Process the search queue batch in order to update the search index
		return batch.process();
	}

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 *
	 * @param fromVersion
	 *            microschema container version to start from
	 * @param toVersion
	 *            microschema container version to end with
	 * @param statusMBean
	 *            JMX Status bean
	 * @return
	 */
	public Observable<Void> migrateMicronodes(MicroschemaContainerVersion fromVersion, MicroschemaContainerVersion toVersion,
			NodeMigrationStatus statusMBean) {

		// get the nodes, that need to be transformed
		List<? extends Micronode> micronodes = db.noTrx(fromVersion::getMicronodes);

		// no micronodes, migration is done
		if (micronodes.isEmpty()) {
			return Observable.just(null);
		}

		if (statusMBean != null) {
			statusMBean.setTotalNodes(micronodes.size());
		}

		// collect the migration scripts
		List<Tuple<String, List<Tuple<String, Object>>>> migrationScripts = new ArrayList<>();
		Set<String> touchedFields = new HashSet<>();
		try (NoTrx noTrx = db.noTrx()) {
			prepareMigration(fromVersion, migrationScripts, touchedFields);
		} catch (IOException e) {
			return Observable.error(e);
		}

		for (Micronode micronode : micronodes) {
			Exception e = db.trx(() -> {
				try {
					NodeMigrationActionContextImpl ac = new NodeMigrationActionContextImpl();
					NodeGraphFieldContainer container = micronode.getContainer();
					if (container == null) {
						throw new Exception("Found micronode without container");
					}
					ac.setLanguageTags(Arrays.asList(container.getLanguage().getLanguageTag()));

					MicronodeResponse restModel = micronode.transformToRestSync(ac, 0).toBlocking().last();

					// Migrate the micronode
					migrate(ac, micronode, restModel, toVersion, touchedFields, migrationScripts, MicronodeResponse.class);

					//TODO do we need to update the search queue batch? Micrnodes are not searchable at the moment.
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
	public Observable<SearchQueueBatch> migrateNodes(Release newRelease) {
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
				if (!node.getGraphFieldContainers(newRelease, Type.INITIAL).isEmpty()) {
					return null;
				}
				node.getGraphFieldContainers(oldRelease, Type.DRAFT).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl initialEdge = node.getImpl().addFramedEdge(HAS_FIELD_CONTAINER,
							container.getImpl(), GraphFieldContainerEdgeImpl.class);
					initialEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					initialEdge.setType(Type.INITIAL);
					initialEdge.setReleaseUuid(newReleaseUuid);

					GraphFieldContainerEdgeImpl draftEdge = node.getImpl().addFramedEdge(HAS_FIELD_CONTAINER,
							container.getImpl(), GraphFieldContainerEdgeImpl.class);
					draftEdge.setLanguageTag(container.getLanguage().getLanguageTag());
					draftEdge.setType(Type.DRAFT);
					draftEdge.setReleaseUuid(newReleaseUuid);

					container.addIndexBatchEntry(batch, STORE_ACTION, newReleaseUuid, Type.DRAFT);
				});

				node.getGraphFieldContainers(oldRelease, Type.PUBLISHED).stream().forEach(container -> {
					GraphFieldContainerEdgeImpl edge = node.getImpl().addFramedEdge(HAS_FIELD_CONTAINER,
							container.getImpl(), GraphFieldContainerEdgeImpl.class);
					edge.setLanguageTag(container.getLanguage().getLanguageTag());
					edge.setType(Type.PUBLISHED);
					edge.setReleaseUuid(newReleaseUuid);

					container.addIndexBatchEntry(batch, STORE_ACTION, newReleaseUuid, Type.PUBLISHED);
				});

				Node parent = node.getParentNode(oldReleaseUuid);
				if (parent != null) {
					node.setParentNode(newReleaseUuid, parent);
				}

				// TODO migrate tags
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
