package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.relationship.GraphRelationship;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.AbstractVertexFrame;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.parameter.SortingParameters;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**BranchRootImpl
 * @see MeshVertex
 */
@GraphElement
public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex, HibBaseElement {

	private static final Logger log = LoggerFactory.getLogger(MeshVertexImpl.class);

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
			String[] fieldParts = key.split("fields\\.");
			String newKey = StringUtils.EMPTY;
			if (fieldParts.length > 1) {
				for (int i = 0 ; i < fieldParts.length; i++) {
					String part = fieldParts[i];
					String[] keyParts = part.split("\\.");
					if (keyParts.length > 1) {
						HibSchema schema = Tx.get().schemaDao().findByName(keyParts[0]);
						String type;
						if (schema != null) {
							FieldSchema field = schema.getLatestVersion().getSchema().getFieldsAsMap().get(keyParts[1]);
							if (field == null) {
								throw new IllegalArgumentException("No field found for sorting request: " + key);
							}
							type = field.getType().toLowerCase();
						} else {
							HibMicroschema microschema = Tx.get().microschemaDao().findByName(keyParts[0]);
							if (microschema != null) {
								//
								FieldSchema field = microschema.getLatestVersion().getSchema().getFieldsAsMap().get(keyParts[1]);
								if (field == null) {
									throw new IllegalArgumentException("No field found for sorting request: " + key);
								}
								type = field.getType().toLowerCase();
							} else {
								throw new IllegalArgumentException("No schema found for sorting request: " + key);
							}
						}
						if (i > 0 && newKey.length() > 0) {
							newKey += ".";
						}
						newKey += "fields." + keyParts[0] + "." + keyParts[1] + "-" + type;
						if (keyParts.length > 2) {
							newKey += Arrays.stream(keyParts).skip(2).map(this::mapGraphQlFieldNameForSorting).collect(Collectors.joining(".", ".", StringUtils.EMPTY));
						}
					}
				}
			} else {
				newKey = key;
			}
			sorting.putSort(newKey,  entry.getValue());
		});
		return sorting;
	}

	@Override
	public Optional<String> permissionFilter(HibUser user, InternalPermission permission, Optional<String> maybeOwner, Optional<ContainerType> containerType) {
		if (user.isAdmin()) {
			return Optional.empty();
		}
		List<InternalPermission> perms;
		if (permission == InternalPermission.READ_PUBLISHED_PERM && containerType.filter(ContainerType.PUBLISHED::equals).isPresent()) {
			perms = Arrays.asList(permission, InternalPermission.READ_PERM);
		} else {
			perms = Collections.singletonList(permission);
		}
		String roleUuids = StreamUtil.toStream(GraphDBTx.getGraphTx().userDao().getRoles(user)).map(role -> String.format("'%s'", role.getUuid())).collect(Collectors.joining(",", "[", "]"));
		return Optional.of(perms.stream().map(perm -> String.format(" ( %s%s CONTAINSANY %s) ", 
				maybeOwner.map(owner -> owner + ".").orElse(StringUtils.EMPTY), perm.propertyKey(), roleUuids)).collect(Collectors.joining(" OR ")));
	}

	@Override
	@SuppressWarnings("rawtypes")
	public String parseFilter(FilterOperation<?> filter, ContainerType ctype) {
		StringBuilder sb = new StringBuilder();
		if (filter.shouldBracket()) {
			sb.append(" ( ");
		}
		String parsedFilter = filter.maybeCombination()
			// If combination, parse each distinctly
			.map(filters -> filters.stream().map(f -> parseFilter(f, ctype)).collect(Collectors.joining(" " + filter.getOperator() + " ")))
			.orElseGet(() -> filter.maybeComparison().map(filters -> {
				FilterOperand<?> left = filters.first;
				FilterOperand<?> right = filters.second;

				// Left comparison operand
				Pair<Class, String> leftJoin = joinIntoPair(new JoinPart(left.maybeGetOwner().orElse(StringUtils.EMPTY), String.valueOf(left.getValue())));
				Object[] leftValue = new Object[] {leftJoin != null ? leftJoin.getValue() : String.valueOf(getFilterOperandValue(left))};

				// Currently we don't support joins in right operand.
				Object qlRight = right.getJoins().stream().findAny().map(unsupported -> {
					throw new IllegalArgumentException("Joins for filter RVALUE are currently unsupported: " + unsupported);
				}).orElseGet(() -> {
					return getFilterOperandValue(right);
				});
				String qlLeft = left.getJoins().stream().map(join -> {
					Pair<Class, String> src = joinIntoPair(join.getLeft());
					Pair<Class, String> dst = joinIntoPair(join.getRight());

					// Case for schema name mapped
					if (src == null || dst == null) {
						return null;
					}
					if (NodeContent.class.equals(dst.getLeft())) {
						// referencedBy
						// TODO union list items like `(inE('HAS_FIELD').outV().inE('HAS_FIELD_CONTAINER').outV().CONDITION) OR (inE('HAS_ITEM').outV().inE('HAS_LIST').outV().inE('HAS_FIELD_CONTAINER').outV().CONDITION)`
						leftValue[0] = "inE('" + HAS_FIELD + "').outV().inE('" + HAS_FIELD_CONTAINER + "').outV().`" + dst.getRight() + "`"  + filter.getOperator() + " " + qlRight 
								+ "  OR "
								+ "inE('" + HAS_ITEM + "').outV().inE('" + HAS_LIST + "').outV().inE('" + HAS_FIELD_CONTAINER + "').outV().`" + dst.getRight() + "`";
						return StringUtils.EMPTY;
					} else if (Collection.class.equals(dst.getLeft()) && "count".equals(dst.getRight())) {
						// TODO FIXME count case
						leftValue[0] = " count(" + left.getValue() + ") ";
					} else if (NodeGraphFieldContainerImpl.class.equals(dst.getLeft())) {
						// If this field is joined to the content, we bypass the join in favor of edge navigation.
						if ("fields".equals(dst.getRight())) {
							// fields case
							// Looking for a CONTENT/<schema_name> = <schema_name>.<field_name>/<field_type> mapping
							String typeSuffix = left.getJoins().stream()
									.filter(e -> "CONTENT".equals(e.getLeft().getTable()) && e.getRight().getTable().equals(e.getLeft().getField() + "." + left.getValue()))
									.map(e -> "-" + e.getRight().getField()).findAny().orElse(StringUtils.EMPTY);
							leftValue[0] = "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ctype.getCode() + "'].inV()[0].`" + left.getValue() + typeSuffix + "`";
						} else {
							leftValue[0] = "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ctype.getCode() + "'].inV()[0].`" + dst.getRight() + "`";
						}
						return StringUtils.EMPTY;
					}

					// If we filter over key column (uuid most of the time), there is no need to join - use the field directly
					if (left.getValue().equals(dst.getRight())) {
						leftValue[0] = src.getRight();
						return StringUtils.EMPTY; 
					} else {
						// Otherwise we mimic joins (unsupported in OrientDB) with subqueries
						String srcField = src.getRight();
						Map<String, GraphRelationship> srcRelations = GraphRelationships.findRelation(src.getKey());
						if (srcRelations != null) {
							GraphRelationship relation = srcRelations.get(src.getValue());
							if (relation != null) {
								if (relation.getRelatedVertexClass() != dst.getKey()) {
									log.error("Mismatch in requested and found relations for {}.{}: requested {}, found {}", src.getKey(), src.getValue(), dst.getKey(), relation.getRelatedVertexClass());
									// TODO throw?
								} else {
									// ctype may be null for Language, but Language should not reach this point.
									srcField = relation.getEdgeFieldName().replace("[edgeType='" + ContainerType.INITIAL.getCode() + "']", "[edgeType='" + ctype.getCode() + "']");
								}
							}
						}
						return " " + srcField + " IN ( SELECT " + dst.getRight() + " FROM " + dst.getLeft().getSimpleName() + " WHERE ";
					}
				}).filter(ql -> !Objects.isNull(ql)).limit(1).collect(Collectors.joining());
				return qlLeft + leftValue[0] + " " + filter.getOperator() + " " + qlRight + (StringUtils.isNotBlank(qlLeft) ? " ) " : "");
			}).orElseGet(() -> filter.toSql()));
		sb.append(parsedFilter);
		if (filter.shouldBracket()) {
			sb.append(" ) ");
		}
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private Object getFilterOperandValue(FilterOperand<?> op) {
		// Special case for date/time values
		if (op.getValue() instanceof Instant) {
			return Instant.class.cast(op.getValue()).toEpochMilli();
		} else if (op.getValue() instanceof Iterable && Iterable.class.cast(op.getValue()).iterator().next() instanceof Instant) {
			return StreamSupport.stream(Iterable.class.cast(op.getValue()).spliterator(), false).map(v -> Long.toString(Instant.class.cast(v).toEpochMilli())).collect(Collectors.joining(",", "[", "]"));
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
			case "LIST":
				return Pair.of(Collection.class, mapGraphQlFieldName(join.getField()));
			case "REFERENCELIST":
				return Pair.of(NodeContent.class, mapGraphQlFieldName(join.getField()));
			default:
				log.warn("Unsupported join item: " + join);
				return null;
			}
		});		
	}
}
