package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.BRANCH_UUID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.EDGE_TYPE_KEY;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static com.gentics.mesh.search.index.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.digest.DigestUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.branch.BranchMicroschemaEdge;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.microschema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.search.index.Bucket;
import com.tinkerpop.blueprints.Direction;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SchemaContainerVersion
 */
public class SchemaContainerVersionImpl extends
	AbstractGraphFieldSchemaContainerVersion<SchemaResponse, SchemaModel, SchemaReference, SchemaContainerVersion, SchemaContainer> implements
	SchemaContainerVersion {

	private static final Logger log = LoggerFactory.getLogger(SchemaContainerVersionImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(SchemaContainerVersionImpl.class, MeshVertexImpl.class);
	}

	@Override
	protected Class<? extends SchemaContainerVersion> getContainerVersionClass() {
		return SchemaContainerVersionImpl.class;
	}

	@Override
	protected Class<? extends SchemaContainer> getContainerClass() {
		return SchemaContainerImpl.class;
	}

	@Override
	public Iterator<? extends NodeGraphFieldContainer> getDraftFieldContainers(String branchUuid) {
		return toStream(mesh().database().getVertices(
			NodeGraphFieldContainerImpl.class,
			new String[] { SCHEMA_CONTAINER_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() })).filter(
				v -> toStream(v.getEdges(Direction.IN, HAS_FIELD_CONTAINER))
					.anyMatch(
						e -> e.getProperty(BRANCH_UUID_KEY).equals(branchUuid) && ContainerType.get(e.getProperty(EDGE_TYPE_KEY)).equals(DRAFT)))
				.map(v -> graph.frameElementExplicit(v, NodeGraphFieldContainerImpl.class)).iterator();
	}

	@Override
	public TraversalResult<? extends Node> getNodes(String branchUuid, User user, ContainerType type) {
		return new TraversalResult<>(getSchemaContainer().getNodes().stream()
			.filter(node -> GraphFieldContainerEdgeImpl.matchesBranchAndType(node.getId(), branchUuid, type)
				&& user.hasPermissionForId(node.getId(), READ_PUBLISHED_PERM)));
	}

	@Override
	public Stream<NodeGraphFieldContainerImpl> getFieldContainers(String branchUuid) {
		return toStream(mesh().database().getVertices(
			NodeGraphFieldContainerImpl.class,
			new String[] { SCHEMA_CONTAINER_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() })).filter(
				v -> toStream(v.getEdges(Direction.IN, HAS_FIELD_CONTAINER))
					.anyMatch(e -> e.getProperty(BRANCH_UUID_KEY).equals(branchUuid)))
				.map(v -> graph.frameElementExplicit(v, NodeGraphFieldContainerImpl.class));
	}

	@Override
	public Stream<NodeGraphFieldContainerImpl> getFieldContainers(String branchUuid, Bucket bucket) {
		return toStream(mesh().database().getVerticesForRange(
			NodeGraphFieldContainerImpl.class,
			"bucket",
			new String[] { SCHEMA_CONTAINER_VERSION_KEY_PROPERTY },
			new Object[] { getUuid() }, BUCKET_ID_KEY, (long) bucket.start(), (long) bucket.end())).filter(
				v -> toStream(v.getEdges(Direction.IN, HAS_FIELD_CONTAINER))
					.anyMatch(e -> e.getProperty(BRANCH_UUID_KEY).equals(branchUuid)))
				.map(v -> graph.frameElementExplicit(v, NodeGraphFieldContainerImpl.class));
	}

	@Override
	public SchemaModel getSchema() {
		SchemaModel schema = mesh().serverSchemaStorage().getSchema(getName(), getVersion());
		if (schema == null) {
			schema = JsonUtil.readValue(getJson(), SchemaModelImpl.class);
			mesh().serverSchemaStorage().addSchema(schema);
		}
		return schema;
	}

	@Override
	public SchemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		// Load the schema and add/overwrite some properties
		// Use getSchema to utilise the schema storage
		SchemaResponse restSchema = JsonUtil.readValue(getJson(), SchemaResponse.class);
		SchemaContainer container = getSchemaContainer();
		container.fillCommonRestFields(ac, fields, restSchema);
		restSchema.setRolePerms(container.getRolePermissions(ac, ac.getRolePermissionParameters().getRoleUuid()));
		return restSchema;

	}

	@Override
	public void setSchema(SchemaModel schema) {
		mesh().serverSchemaStorage().removeSchema(schema.getName(), schema.getVersion());
		mesh().serverSchemaStorage().addSchema(schema);
		String json = schema.toJson();
		setJson(json);
		setProperty(VERSION_PROPERTY_KEY, schema.getVersion());
	}

	@Override
	public SchemaReferenceImpl transformToReference() {
		SchemaReferenceImpl reference = new SchemaReferenceImpl();
		reference.setName(getName());
		reference.setUuid(getSchemaContainer().getUuid());
		reference.setVersion(getVersion());
		reference.setVersionUuid(getUuid());
		return reference;
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return "";
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	public TraversalResult<? extends Branch> getBranches() {
		return in(HAS_SCHEMA_VERSION, BranchImpl.class);
	}

	@Override
	public Iterable<Job> referencedJobsViaTo() {
		return in(HAS_TO_VERSION).frame(Job.class);
	}

	@Override
	public TraversalResult<Job> referencedJobsViaFrom() {
		return new TraversalResult<>(in(HAS_FROM_VERSION).frame(Job.class));
	}

	@Override
	public void delete(BulkActionContext context) {
		generateUnassignEvents().forEach(context::add);
		// Delete change
		SchemaChange<?> change = getNextChange();
		if (change != null) {
			change.delete(context);
		}
		// Delete referenced jobs
		for (Job job : referencedJobsViaFrom()) {
			job.remove();
		}
		for (Job job : referencedJobsViaTo()) {
			job.remove();
		}
		// Delete version
		remove();
	}

	/**
	 * Genereates branch unassign events for every assigned branch.
	 * 
	 * @return
	 */
	private Stream<BranchSchemaAssignEventModel> generateUnassignEvents() {
		return getBranches().stream()
			.map(branch -> branch.onSchemaAssignEvent(this, UNASSIGNED, null, null));
	}

	@Override
	public MeshElementEventModel onCreated() {
		return getSchemaContainer().onCreated();
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return getSchemaContainer().onUpdated();
	}

	@Override
	public boolean isAutoPurgeEnabled() {
		Boolean schemaAutoPurge = getSchema().getAutoPurge();
		if (schemaAutoPurge == null) {
			if (log.isDebugEnabled()) {
				log.debug("No schema auto purge flag set. Falling back to mesh global setting");
			}
			ContentConfig contentOptions = options().getContentOptions();
			if (contentOptions != null) {
				return contentOptions.isAutoPurge();
			} else {
				return true;
			}
		} else {
			return schemaAutoPurge;
		}
	}

	@Override
	public String getMicroschemaVersionHash(Branch branch, Map<String, String> replacementMap) {
		Objects.requireNonNull(branch, "The branch must not be null");
		Objects.requireNonNull(replacementMap, "The replacement map must not be null (but may be empty)");
		Set<String> microschemaNames = getSchema().getFields().stream().filter(field -> FieldTypes.valueByName(field.getType()) == FieldTypes.MICRONODE).flatMap(field -> {
			String[] allowed = ((MicronodeFieldSchema) field).getAllowedMicroSchemas();
			return Stream.of(allowed);
		}).collect(Collectors.toSet());

		if (microschemaNames.isEmpty()) {
			return null;
		} else {
			Set<String> microschemaVersionUuids = new TreeSet<>();
			for (BranchMicroschemaEdge edge : branch.findAllLatestMicroschemaVersionEdges()) {
				MicroschemaContainerVersion version = edge.getMicroschemaContainerVersion();
				MicroschemaModel microschema = version.getSchema();
				String microschemaName = microschema.getName();

				// if the microschema is one of the "used" microschemas, we either get the version uuid from the replacement map, or
				// the uuid of the currently assigned version
				if (microschemaNames.contains(microschemaName)) {
					microschemaVersionUuids.add(replacementMap.getOrDefault(microschemaName, version.getUuid()));
				}
			}

			if (microschemaVersionUuids.isEmpty()) {
				return null;
			} else {
				return DigestUtils.md5Hex(microschemaVersionUuids.stream().collect(Collectors.joining("|")));
			}
		}
	}

	@Override
	public Set<String> getFieldsUsingMicroschema(MicroschemaContainer microschema) {
		return getSchema().getFields().stream().filter(field -> {
			if (FieldTypes.valueByName(field.getType()) == FieldTypes.MICRONODE) {
				MicronodeFieldSchema micronodeField = (MicronodeFieldSchema) field;
				return Arrays.asList(micronodeField.getAllowedMicroSchemas()).contains(microschema.getName());
			} else if (FieldTypes.valueByName(field.getType()) == FieldTypes.LIST) {
				ListFieldSchema listField = (ListFieldSchema) field;
				return FieldTypes.valueByName(listField.getListType()) == FieldTypes.MICRONODE
						&& Arrays.asList(listField.getAllowedSchemas()).contains(microschema.getName());
			} else {
				return false;
			}
		}).map(FieldSchema::getName).collect(Collectors.toSet());
	}

	@Override
	public boolean usesMicroschema(MicroschemaContainer microschema) {
		return getSchema().getFields().stream()
				.filter(field -> FieldTypes.valueByName(field.getType()) == FieldTypes.MICRONODE)
				.filter(field -> Arrays.asList(((MicronodeFieldSchema) field).getAllowedMicroSchemas())
						.contains(microschema.getName())).findFirst().isPresent();
	}
}
