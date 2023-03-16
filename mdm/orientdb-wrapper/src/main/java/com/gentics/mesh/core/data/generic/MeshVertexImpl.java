package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.*;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.graphqlfilter.filter.operation.JoinPart;
import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.LanguageImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.job.impl.JobImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.db.AbstractVertexFrame;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.MeshOrientGraphQuery;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.parameter.SortingParameters;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;

/**BranchRootImpl
 * @see MeshVertex
 */
@GraphElement
public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex, HibBaseElement {

	private String uuid;

	/**
	 * Initialize the vertex type and index.
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MeshVertexImpl.class, null);
		index.createIndex(vertexIndex(MeshVertexImpl.class)
			.withField("uuid", FieldType.STRING)
			.unique());
	}

	/**
	 * Add a default user tracking relation, where applicable.
	 * 
	 * @param <K>
	 * @param keyClass
	 */
	public static <K extends MeshVertex> void addUserTrackingRelation(Class<K> keyClass) {
		GraphRelationships.addRelation(keyClass, UserImpl.class, "creator");
		GraphRelationships.addRelation(keyClass, UserImpl.class, "editor");
	}

	@Override
	protected void init(FramedGraph graph, Element element, Object id) {
		super.init(graph, null, id);
	}

	@Override
	protected void init() {
		super.init();
		property("uuid", UUIDUtil.randomUUID());
	}

	/**
	 * Return the properties which are prefixed using the given key.
	 * 
	 * @param prefix
	 *            Property prefix
	 * @return Found properties
	 */
	public <T> Map<String, T> getProperties(String prefix) {
		Map<String, T> properties = new HashMap<>();

		for (String key : getPropertyKeys()) {
			if (key.startsWith(prefix)) {
				properties.put(key, getProperty(key));
			}
		}
		return properties;
	}

	@Getter
	public String getUuid() {
		// Return the locally stored uuid if possible. Otherwise load it from the graph.
		if (uuid == null) {
			this.uuid = property("uuid");
		}
		return uuid;
	}

	@Setter
	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
		this.uuid = uuid;
	}

	public Vertex getVertex() {
		return getElement();
	}

	public String getFermaType() {
		return property(TYPE_RESOLUTION_KEY);
	}

	@Override
	public FramedGraph getGraph() {
		return GraphDBTx.getGraphTx().getGraph();
	}

	@Override
	public void delete(BulkActionContext context) {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
	}

	@Override
	public String getElementVersion() {
		Vertex vertex = getElement();
		return mesh().database().getElementVersion(vertex);
	}

	@Override
	public void setCachedUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * Return the dagger mesh context from the graph attributes. The component is accessed this way since it is not otherwise possible to inject dagger into
	 * domain classes.
	 * 
	 * @return
	 */
	public OrientDBMeshComponent mesh() {
		return getGraphAttribute(GraphAttribute.MESH_COMPONENT);
	}

	/**
	 * Return the public mesh API
	 * 
	 * @return
	 */
	public Mesh meshApi() {
		return mesh().boot().mesh();
	}

	/**
	 * Return the Mesh options.
	 */
	public MeshOptions options() {
		return mesh().options();
	}

	@Override
	public GraphDatabase db() {
		return mesh().database();
	}

	@Override
	public Vertx vertx() {
		return mesh().vertx();
	}

	/**
	 * Return the used vertx (rx variant) instance for mesh.
	 * 
	 * @return Rx Vertx instance
	 */
	public io.vertx.reactivex.core.Vertx rxVertx() {
		return new io.vertx.reactivex.core.Vertx(vertx());
	}

	@Override
	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
		Set<String> oset = property(permission.propertyKey());
		if (oset == null) {
			return new HashSet<>(10);
		} else {
			return new HashSet<>(oset);
		}
	}

	@Override
	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
		property(permission.propertyKey(), allowedRoles);
	}
	/**
	 * Map default sorting argument to the one acceptable by {@link MeshOrientGraphQuery}.
	 * 
	 * @param <S>
	 * @param sorting
	 * @return
	 */
	protected <S extends SortingParameters> S mapSorting(S sorting) {
		if (sorting == null) {
			return sorting;
		}
		Map<String, SortOrder> items = sorting.getSort();
		sorting.clearSort();
		items.entrySet().stream().forEach(entry -> {
			String key = mapGraphQlFieldNameForSorting(entry.getKey());
			String[] keyParts = key.split("\\.");
			if (keyParts.length > 1) {
				// We expect format of 'fields.<schema_name>'.<field_name>
				if ("fields".equals(keyParts[0])) {
					if (keyParts.length > 2) {
						HibSchema schema = Tx.get().schemaDao().findByName(keyParts[1]);
						if (schema == null) {
							throw new IllegalArgumentException("No schema found for sorting request: " + key);
						}
						FieldSchema field = schema.getLatestVersion().getSchema().getFieldsAsMap().get(keyParts[2]);
						if (field == null) {
							throw new IllegalArgumentException("No field found for sorting request: " + key);
						}
						key = keyParts[0] + "." + keyParts[2] + "-" + field.getType().toLowerCase();
					}
					// else process as non-schema content field
				} else if ("creator".equals(keyParts[0]) || "editor".equals(keyParts[0])) {
					// pass it
				} else {
					throw new IllegalArgumentException("The sort key is currently not supported: " + key + ". Either an entity field (e.g. 'uuid') or node fields (e.g. 'fields.<schema_name>.<field_name>') are implemented.");
				}
			}
			sorting.putSort(key,  entry.getValue());
		});
		return sorting;
	}

	/**
	 * Parse a filter operation into the OrientDB's WHERE clause.
	 * 
	 * @param filter
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public String parseFilter(FilterOperation<?> filter) {
		StringBuilder sb = new StringBuilder();
		sb.append(" ( ");
		String parsedFilter = filter.maybeCombination()
			// If combination, parse each distinctly
			.map(filters -> filters.stream().map(this::parseFilter).collect(Collectors.joining(" " + filter.getOperator() + " ")))
			.orElseGet(() -> filter.maybeComparison().map(filters -> {
				FilterOperand<?> left = filters.first;
				FilterOperand<?> right = filters.second;

				// Left comparison operand
				Pair<Class, String> leftJoin = joinIntoPair(new JoinPart(left.maybeGetOwner().orElse(StringUtils.EMPTY), String.valueOf(left.getValue())));
				Object[] leftValue = new Object[] {leftJoin != null ? leftJoin.getValue() : String.valueOf(getFilterOperandValue(left))};

				String qlLeft = left.getJoins().entrySet().stream().map(join -> {
					Pair<Class, String> src = joinIntoPair(join.getKey());
					Pair<Class, String> dst = joinIntoPair(join.getValue());

					// Case for schema name mapped
					if (src == null || dst == null) {
						return null;
					}

					// If this field is joined to the content, we bypass the join in favor of edge navigation.
					// TODO customize container type (currently PUBLISHED is hardcoded)
					if (NodeGraphFieldContainerImpl.class.equals(dst.getLeft())) {
						if ("fields".equals(dst.getRight())) {
							// Looking for a CONTENT/<schema_name> = <schema_name>.<field_name>/<field_type> mapping
							String typeSuffix = left.getJoins().entrySet().stream()
									.filter(e -> "CONTENT".equals(e.getKey().getTable()) && e.getValue().getTable().equals(e.getKey().getField() + "." + left.getValue()))
									.map(e -> "-" + e.getValue().getField()).findAny().orElse(StringUtils.EMPTY);
							//String typeSuffix = left.getJoins().entrySet().stream().map(e -> joinIntoPair(e.getValue())).filter(e -> e.getKey() == null).map(e -> "-" + e.getValue()).findAny().orElse(StringUtils.EMPTY);
							leftValue[0] = "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ContainerType.PUBLISHED.getCode() + "'].inV()[0].`" + left.getValue() + typeSuffix + "`";
						} else {
							leftValue[0] = "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ContainerType.PUBLISHED.getCode() + "'].inV()[0].`" + dst.getRight() + "`";
						}
						return StringUtils.EMPTY;
					}

					// If we filter over key column (uuid most of the time), there is no need to join - use the field directly
					if (left.getValue().equals(dst.getRight())) {
						leftValue[0] = src.getRight();
						return StringUtils.EMPTY; 
					} else {
						// Otherwise we mimic joins (unsupported in OrientDB) with subqueries
						return " " + src.getRight() + " IN ( SELECT " + dst.getRight() + " FROM " + dst.getLeft().getSimpleName() + " WHERE ";
					}
				}).filter(ql -> !Objects.isNull(ql)).limit(1).collect(Collectors.joining());

				// Currently we don't support joins in right operand.
				Object qlRight = right.getJoins().entrySet().stream().findAny().map(unsupported -> {
					throw new IllegalArgumentException("Joins for filter RVALUE are currently unsupported: " + unsupported);
				}).orElseGet(() -> {
					return getFilterOperandValue(right);
				});
				return qlLeft + leftValue[0] + " " + filter.getOperator() + " " + qlRight + (StringUtils.isNotBlank(qlLeft) ? " ) " : "");
			}).orElseThrow(() -> new IllegalStateException("Filter " + filter.getOperator() + " is neither combination nor comparison.")));
		sb.append(parsedFilter);
		sb.append(" ) ");
		return sb.toString();
	}

	private Object getFilterOperandValue(FilterOperand<?> op) {
		// Special case for date/time values
		if (op.getValue() instanceof Instant) {
			return Instant.class.cast(op.getValue()).toEpochMilli();
		} else {
			return op.toSql();
		}
	}

	/**
	 * Override this method to map a GraphQL entity field name to its OrientDB counterpart.
	 * 
	 * @param gqlName
	 * @return
	 */
	public String mapGraphQlFieldName(String gqlName) {
		return gqlName;
	}

	/**
	 * Override this method to map a GraphQL entity field name to its OrientDB counterpart, specially prepared for a sorting. 
	 * Equals to {@link MeshVertexImpl#mapGraphQlFieldName(String)} most of the time.
	 * 
	 * @param gqlName
	 * @return
	 */
	public String mapGraphQlFieldNameForSorting(String gqlName) {
		return mapGraphQlFieldName(gqlName);
	}

	/**
	 * Parse join into pair of Mesh element class and field.
	 * 
	 * @see ElementType
	 * @see MeshElement
	 * 
	 * @param join
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public Pair<Class, String> joinIntoPair(JoinPart join) {
		return ElementType.parse(join.getTable()).map(etype -> {
			Class clss;
			switch (etype) {
			case BRANCH:
				clss = BranchImpl.class;
				break;
			case GROUP:
				clss = GroupImpl.class;
				break;
			case JOB:
				clss = JobImpl.class;
				break;
			case LANGUAGE:
				clss = LanguageImpl.class;
				break;
			case MICROSCHEMA:
				clss = MicroschemaContainerImpl.class;
				break;
			case MICROSCHEMAVERSION:
				clss = MicroschemaContainerVersionImpl.class;
				break;
			case NODE:
				clss = NodeImpl.class;
				break;
			case PROJECT:
				clss = ProjectImpl.class;
				break;
			case ROLE:
				clss = RoleImpl.class;
				break;
			case SCHEMA:
				clss = SchemaContainerImpl.class;
				break;
			case SCHEMAVERSION:
				clss = SchemaContainerVersionImpl.class;
				break;
			case TAG:
				clss = TagImpl.class;
				break;
			case TAGFAMILY:
				clss = TagFamilyImpl.class;
				break;
			case USER:
				clss = UserImpl.class;
				break;
			default:
				throw new IllegalStateException("FIXME: unexpected element type, that should be supported " + etype);
			}
			return Pair.of(clss, mapGraphQlFieldName(join.getField()));
		}).orElseGet(() -> {
			switch (join.getTable()) {
			case "CONTENT":
				return Pair.of(NodeGraphFieldContainerImpl.class, mapGraphQlFieldName(join.getField()));
			case "MICRONODE":
				return Pair.of(MicroschemaContainerImpl.class, mapGraphQlFieldName(join.getField()));
			default:
				return null;
			}
		});		
	}
}
