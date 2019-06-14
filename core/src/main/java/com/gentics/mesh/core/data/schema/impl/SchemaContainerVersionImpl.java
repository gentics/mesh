package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FROM_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PARENT_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TO_VERSION;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.etc.config.ContentConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.VertexFrame;

import io.reactivex.Single;
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
		return in(HAS_SCHEMA_CONTAINER_VERSION).inE(HAS_FIELD_CONTAINER).filter(e -> {
			GraphFieldContainerEdgeImpl edge = e.reframeExplicit(GraphFieldContainerEdgeImpl.class);
			ContainerType type = edge.getType();
			return branchUuid.equals(edge.getBranchUuid()) && (DRAFT == type);
		}).inV().frameExplicit(NodeGraphFieldContainerImpl.class).iterator();
	}

	@Override
	public Iterable<? extends Node> getNodes(String branchUuid, User user, ContainerType type) {
		return in(HAS_PARENT_CONTAINER).in(HAS_SCHEMA_CONTAINER).transform(v -> v.reframeExplicit(NodeImpl.class)).filter(node -> {
			return node.outE(HAS_FIELD_CONTAINER).filter(e -> {
				GraphFieldContainerEdge edge = e.reframeExplicit(GraphFieldContainerEdgeImpl.class);
				return branchUuid.equals(edge.getBranchUuid()) && type == edge.getType();
			}).hasNext() && user.hasPermissionForId(node.id(), READ_PUBLISHED_PERM);
		});
	}

	@Override
	public Stream<NodeGraphFieldContainer> getFieldContainers(String branchUuid) {
		Spliterator<VertexFrame> it = in(HAS_SCHEMA_CONTAINER_VERSION).spliterator();
		Stream<NodeGraphFieldContainer> stream = StreamSupport.stream(it, false).map(frame -> frame.reframe(NodeGraphFieldContainerImpl.class))
			.filter(e -> e.getParentNode(branchUuid) != null).map(e -> (NodeGraphFieldContainer) e);
		return stream;
	}

	@Override
	public SchemaModel getSchema() {
		SchemaModel schema = MeshInternal.get().serverSchemaStorage().getSchema(getName(), getVersion());
		if (schema == null) {
			schema = JsonUtil.readValue(getJson(), SchemaModelImpl.class);
			MeshInternal.get().serverSchemaStorage().addSchema(schema);
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
		container.setRolePermissions(ac, restSchema);
		return restSchema;

	}

	@Override
	public void setSchema(SchemaModel schema) {
		MeshInternal.get().serverSchemaStorage().removeSchema(schema.getName(), schema.getVersion());
		MeshInternal.get().serverSchemaStorage().addSchema(schema);
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
	public String getETag(InternalActionContext ac) {
		return ETag.hash(getUuid());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	public TraversalResult<? extends Branch> getBranches() {
		return new TraversalResult<>(in(HAS_SCHEMA_VERSION).frameExplicit(BranchImpl.class));
	}

	@Override
	public Single<SchemaResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return MeshInternal.get().database().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
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
			.map(branch -> branch.onSchemaAssignEvent(this, UNASSIGNED, null));
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
			MeshOptions options = Mesh.mesh().getOptions();
			ContentConfig contentOptions = options.getContentOptions();
			if (contentOptions != null) {
				return contentOptions.isAutoPurge();
			} else {
				return true;
			}
		} else {
			return schemaAutoPurge;
		}
	}

}
