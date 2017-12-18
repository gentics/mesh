package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.rest.error.Errors.conflict;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HandleElementAction;
import com.gentics.mesh.core.data.IndexableElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.FieldType;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.syncleus.ferma.tx.Tx;
import com.tinkerpop.blueprints.Edge;

import io.reactivex.Single;

/**
 * @see Role
 */
public class RoleImpl extends AbstractMeshCoreVertex<RoleResponse, Role> implements Role {

	public static void init(Database database) {
		database.addVertexType(RoleImpl.class, MeshVertexImpl.class);
		database.addVertexIndex(RoleImpl.class, true, "name", FieldType.STRING);
	}

	@Override
	public RoleReference transformToReference() {
		return new RoleReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public List<? extends Group> getGroups() {
		return out(HAS_ROLE).toListExplicit(GroupImpl.class);
	}

	@Override
	public Page<? extends Group> getGroups(User user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = out(HAS_ROLE);
		return new DynamicTransformablePageImpl<Group>(user, traversal, pagingInfo, READ_PERM, GroupImpl.class);
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex vertex) {
		Set<GraphPermission> permissions = new HashSet<>();
		for (GraphPermission permission : GraphPermission.values()) {
			if (hasPermission(permission, vertex)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(GraphPermission permission, MeshVertex vertex) {
		FramedGraph graph = Tx.getActive().getGraph();
		Iterable<Edge> edges = graph.getEdges("e." + permission.label().toLowerCase() + "_inout", MeshInternal.get().database().createComposedIndexKey(vertex
				.getId(), getId()));
		return edges.iterator().hasNext();
	}

	@Override
	public void grantPermissions(MeshVertex vertex, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {
			if (!hasPermission(permission, vertex)) {
				addFramedEdge(permission.label(), vertex);
			}
		}
	}

	@Override
	public RoleResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		RoleResponse restRole = new RoleResponse();
		restRole.setName(getName());

		setGroups(ac, restRole);
		fillCommonRestFields(ac, restRole);
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
		FramedGraph graph = Tx.getActive().getGraph();
		Object indexKey = MeshInternal.get().database().createComposedIndexKey(vertex.getId(), getId());

		long edgesRemoved = Arrays.stream(permissions).map(perm -> "e." + perm.label().toLowerCase() + "_inout").flatMap(key -> StreamSupport.stream(graph.getEdges(
				key, indexKey).spliterator(), false)).peek(Edge::remove).count();

		if (edgesRemoved > 0) {
			PermissionStore.invalidate();
		}
	}

	/**
	 * Return all vertices to which the role has the given permission.
	 * 
	 * @param perm
	 * @return
	 */
	public Iterable<? extends MeshVertex> getElementsWithPermission(GraphPermission perm) {
		return out(perm.label()).frame(MeshVertexImpl.class);
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO don't allow deletion of admin role
		batch.delete(this, true);
		// Update all document in the index which reference the uuid of the role
		for (GraphPermission perm : Arrays.asList(READ_PERM, READ_PUBLISHED_PERM)) {
			for (MeshVertex element : getElementsWithPermission(perm)) {
				// We don't need to update the role itself since it will be purged from the index anyway
				if (element.getUuid().equals(getUuid())) {
					continue;
				}
				if (element instanceof IndexableElement) {
					batch.updatePermissions((IndexableElement) element);
				}
			}
		}
		getVertex().remove();

		PermissionStore.invalidate();
	}

	@Override
	public boolean update(InternalActionContext ac, SearchQueueBatch batch) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		BootstrapInitializer boot = MeshInternal.get().boot();
		if (shouldUpdate(requestModel.getName(), getName())) {
			// Check for conflict
			Role roleWithSameName = boot.roleRoot().findByName(requestModel.getName());
			if (roleWithSameName != null && !roleWithSameName.getUuid().equals(getUuid())) {
				throw conflict(roleWithSameName.getUuid(), requestModel.getName(), "role_conflicting_name");
			}

			setName(requestModel.getName());
			batch.store(this, true);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void handleRelatedEntries(HandleElementAction action) {
		for (Group group : getGroups()) {
			action.call(group, null);
		}
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(super.getETag(ac));
		keyBuilder.append(getLastEditedTimestamp());
		return ETag.hash(keyBuilder);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return "/api/v1/roles/" + getUuid();
	}

	@Override
	public User getCreator() {
		return out(HAS_CREATOR).nextOrDefault(UserImpl.class, null);
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
	}

	@Override
	public Single<RoleResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return DB.get().asyncTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
