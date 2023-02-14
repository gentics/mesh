package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.graphqlfilter.filter.operation.FilterOperand;
import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
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
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;

/**
 * Abstract implementation for root vertices which are aggregation vertices for mesh core vertices. The abstract implementation contains various helper methods
 * that are useful for loading lists and items from the root vertex.
 * 
 * @see RootVertex
 * @param <T>
 */
public abstract class AbstractRootVertex<T extends MeshCoreVertex<? extends RestModel>> extends MeshVertexImpl implements RootVertex<T> {

	@Override
	abstract public Class<? extends T> getPersistanceClass();

	@Override
	abstract public String getRootLabel();

	@Override
	public boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
                                    Set<InternalPermission> permissionsToRevoke) {
		UserDao userDao = Tx.get().userDao();
		boolean permissionChanged = false;
		if (recursive) {
			for (T t : findAll().stream().filter(e -> userDao.hasPermission(authUser.getDelegate(), this, READ_PERM)).collect(Collectors.toList())) {
				permissionChanged = t.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = RootVertex.super.applyPermissions(authUser, batch, toGraph(role), false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}

	@Override
	public PermissionInfo getRolePermissions(HibBaseElement element, InternalActionContext ac, String roleUuid) {
		return Tx.get().roleDao().getRolePermissions(element, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(HibBaseElement vertex, InternalPermission perm) {
		return Tx.get().roleDao().getRolesWithPerm(vertex, perm);
	}

	/**
	 * Update the role permissions for the given vertex.
	 * 
	 * @param vertex
	 * @param ac
	 * @param model
	 */
	public void setRolePermissions(MeshVertex vertex, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(getRolePermissions(vertex, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	/**
	 * Not implemented for abstract implementations.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	public String getAPIPath(T element, InternalActionContext ac) {
		// TODO FIXME remove this method, must be implemented in all derived classes
		throw new RuntimeException("Not implemented");
	}

	/**
	 * Parse a filter operation into the OrientDB's WHERE clause.
	 * 
	 * @param filter
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected String parseFilter(FilterOperation<?> filter) {
		StringBuilder sb = new StringBuilder();
		sb.append(" ( ");
		String parsedFilter = filter.maybeCombination()
			// If combination, parse each distinctly
			.map(filters -> filters.stream().map(this::parseFilter).collect(Collectors.joining(" " + filter.getOperator() + " ")))
			.orElseGet(() -> filter.maybeComparison().map(filters -> {
				FilterOperand<?> left = filters.first;
				FilterOperand<?> right = filters.second;

				// Left comparison operand
				Object[] leftValue = new Object[] {left.getValue()};
				Optional<graphql.util.Pair<String, String>> maybeLeftJoin = left.getJoins().entrySet().stream().map(e -> graphql.util.Pair.pair(e.getKey(), e.getValue())).findAny();
				Optional<graphql.util.Pair<String, String>> maybeRightJoin = right.getJoins().entrySet().stream().map(e -> graphql.util.Pair.pair(e.getKey(), e.getValue())).findAny();

				String qlLeft = maybeLeftJoin.map(join -> {
					Pair<Class, String> src = joinIntoPair(join.first);
					Pair<Class, String> dst = joinIntoPair(join.second);

					// If this field is joined to the content, we bypass the join in favor of edge navigation.
					// TODO customize container type
					if (NodeGraphFieldContainerImpl.class.equals(dst.getLeft())) {
						leftValue[0] = "outE('" + HAS_FIELD_CONTAINER + "')[edgeType='" + ContainerType.PUBLISHED.getCode() + "'].inV()[0].`" + dst.getRight() + "`";
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
				}).orElse(StringUtils.EMPTY);

				// Currently we don't support joins in right operand.
				Object qlRight = maybeRightJoin.map(unsupported -> {
					throw new IllegalArgumentException("Joins for filter RVALUE are currently unsupported: " + unsupported);
				}).orElseGet(() -> {
					// Special case for date/time values
					if (right.getValue() instanceof Instant) {
						return Instant.class.cast(right.getValue()).toEpochMilli();
					} else {
						return right.toSql();
					}
				});
				return qlLeft + leftValue[0] + " " + filter.getOperator() + " " + qlRight + (StringUtils.isNotBlank(qlLeft) ? " ) " : "");
			}).orElseThrow(() -> new IllegalStateException("Filter " + filter.getOperator() + " is neither combination nor comparison.")));
		sb.append(parsedFilter);
		sb.append(" ) ");
		return sb.toString();
	}

	/**
	 * Parse join string in the form of `table.field` into pair of Mesh element class and field.
	 * 
	 * @see ElementType
	 * @see MeshElement
	 * 
	 * @param join
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	protected Pair<Class, String> joinIntoPair(String join) {
		String[] parts = join.split("\\.", 2);
		Class cls = ElementType.parse(parts[0]).map(etype -> {
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
			return clss;
		}).orElseGet(() -> {
			switch (parts[0]) {
			case "CONTENT":
				return NodeGraphFieldContainerImpl.class;
			case "MICRONODE":
				return MicroschemaContainerImpl.class;
			default:
				throw new IllegalArgumentException("Unsupported element type: " + parts[0]);
			}
		});
		return Pair.of(cls, parts[1]);
	}

	/**
	 * Generate the eTag for the element. Every time the edges to the root vertex change the internal element version gets updated.
	 * 
	 * @param element
	 * @param ac
	 * @return
	 */
	public final String getETag(T element, InternalActionContext ac) {
		UserDao userDao = Tx.get().userDao();
		RoleDao roleDao = Tx.get().roleDao();

		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(getUuid());
		keyBuilder.append("-");
		keyBuilder.append(userDao.getPermissionInfo(ac.getUser(), element).getHash());

		keyBuilder.append("fields:");
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		fields.forEach(keyBuilder::append);

		/**
		 * permissions (&roleUuid query parameter aware)
		 *
		 * Permissions can change and thus must be included in the etag computation in order to invalidate the etag once the permissions change.
		 */
		String roleUuid = ac.getRolePermissionParameters().getRoleUuid();
		if (!isEmpty(roleUuid)) {
			HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				Set<InternalPermission> permSet = roleDao.getPermissions(role, element);
				Set<String> humanNames = new HashSet<>();
				for (InternalPermission permission : permSet) {
					humanNames.add(permission.getRestPerm().getName());
				}
				String[] names = humanNames.toArray(new String[humanNames.size()]);
				keyBuilder.append(Arrays.toString(names));
			}

		}

		// Add the type specific etag part
		keyBuilder.append(getSubETag(element, ac));
		return ETag.hash(keyBuilder.toString());
	}

	/**
	 * This method provides the element specific etag. It needs to be individually implemented for all core element classes.
	 *
	 * @param element
	 * @param ac
	 * @return
	 */
	public String getSubETag(T element, InternalActionContext ac) {
		// TODO FIXME make this method abstract
		throw new RuntimeException("Not implemented");
	}

	@Override
	public long globalCount() {
		return db().count(getPersistanceClass());
	}

	@Override
	public T create() {
		return getGraph().addFramedVertex(getPersistanceClass());
	}
}
