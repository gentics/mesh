package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.tx.Tx;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Edge;

import io.reactivex.Single;

/**
 * @see Role
 */
public class RoleImpl extends AbstractMeshCoreVertex<RoleResponse, Role> implements Role {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(RoleImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(RoleImpl.class)
			.withField("name", FieldType.STRING)
			.unique());
	}

	@Override
	public RoleReference transformToReference() {
		return new RoleReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public TraversalResult<? extends Group> getGroups() {
		return out(HAS_ROLE, GroupImpl.class);
	}

	@Override
	public Page<? extends Group> getGroups(User user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = out(HAS_ROLE);
		return new DynamicTransformablePageImpl<Group>(user, traversal, pagingInfo, READ_PERM, GroupImpl.class);
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex vertex) {
		Set<GraphPermission> permissions = new HashSet<>();
		GraphPermission[] possiblePermissions = vertex.hasPublishPermissions()
			? GraphPermission.values()
			: GraphPermission.basicPermissions();

		for (GraphPermission permission : possiblePermissions) {
			if (hasPermission(permission, vertex)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(GraphPermission permission, MeshVertex vertex) {
		Set<String> allowedUuids = vertex.property(permission.propertyKey());
		return allowedUuids != null && allowedUuids.contains(getUuid());
	}

	@Override
	public void grantPermissions(MeshVertex vertex, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {
			Set<String> allowedRoles = vertex.property(permission.propertyKey());
			if (allowedRoles == null) {
				vertex.property(permission.propertyKey(), Collections.singleton(getUuid()));
			} else {
				allowedRoles.add(getUuid());
				vertex.property(permission.propertyKey(), allowedRoles);
			}
		}
	}

	@Override
	public RoleResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		RoleResponse restRole = new RoleResponse();

		if (fields.has("name")) {
			restRole.setName(getName());
		}

		if (fields.has("groups")) {
			setGroups(ac, restRole);
		}
		fillCommonRestFields(ac, fields, restRole);

		setRolePermissions(ac, restRole);
		return restRole;
	}

	private void setGroups(InternalActionContext ac, RoleResponse restRole) {
		for (Group group : getGroups()) {
			restRole.getGroups().add(group.transformToReference());
		}
	}

	@Override
	public void revokePermissions(MeshVertex vertex, GraphPermission... permissions) {
		boolean permissionRevoked = false;
		for (GraphPermission permission : permissions) {
			Set<String> allowedRoles = vertex.property(permission.propertyKey());
			if (allowedRoles != null) {
				permissionRevoked = allowedRoles.remove(getUuid()) || permissionRevoked;
				vertex.property(permission.propertyKey(), allowedRoles);
			}
		}

		if (permissionRevoked) {
			mesh().permissionCache().clear();
		}
	}

	@Override
	public void delete(BulkActionContext bac) {
		bac.add(onDeleted());
		getVertex().remove();
		bac.process();
		mesh().permissionCache().clear();
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		BootstrapInitializer boot = mesh().boot();
		if (shouldUpdate(requestModel.getName(), getName())) {
			// Check for conflict
			Role roleWithSameName = boot.roleRoot().findByName(requestModel.getName());
			if (roleWithSameName != null && !roleWithSameName.getUuid().equals(getUuid())) {
				throw conflict(roleWithSameName.getUuid(), requestModel.getName(), "role_conflicting_name");
			}

			setName(requestModel.getName());
			batch.add(onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/roles/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR, UserImpl.class).nextOrNull();
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR, UserImpl.class).nextOrNull();
	}

	@Override
	public Single<RoleResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
