package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.cache.PermissionStore;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HandleElementAction;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.reactivex.Single;


/**
 * @see Group
 */
public class GroupImpl extends AbstractMeshCoreVertex<GroupResponse, Group> implements Group {

	public static void init(Database database) {
		database.addVertexType(GroupImpl.class, MeshVertexImpl.class);
		database.addVertexIndex(GroupImpl.class, true, "name");
	}

	@Override
	public GroupReference transformToReference() {
		return new GroupReference().setName(getName()).setUuid(getUuid());
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
	public List<? extends User> getUsers() {
		return in(HAS_USER).toListExplicit(UserImpl.class);
	}

	@Override
	public void addUser(User user) {
		setUniqueLinkInTo(user, HAS_USER);

		// Add shortcut edge from user to roles of this group
		for (Role role : getRoles()) {
			user.setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
		}
	}

	@Override
	public void removeUser(User user) {
		unlinkIn(user, HAS_USER);

		// Remove shortcut edge from user to roles of this group
		for (Role role : getRoles()) {
			user.unlinkOut(role, ASSIGNED_TO_ROLE);
		}
		PermissionStore.invalidate();
	}

	@Override
	public List<? extends Role> getRoles() {
		return in(HAS_ROLE).toListExplicit(RoleImpl.class);
	}

	@Override
	public void addRole(Role role) {
		setUniqueLinkInTo(role, HAS_ROLE);

		// Add shortcut edges from role to users of this group
		for (User user : getUsers()) {
			user.setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
		}

	}

	@Override
	public void removeRole(Role role) {
		unlinkIn(role, HAS_ROLE);

		// Remove shortcut edges from role to users of this group
		for (User user : getUsers()) {
			user.unlinkOut(role, ASSIGNED_TO_ROLE);
		}
		PermissionStore.invalidate();
	}

	@Override
	public boolean hasRole(Role role) {
		return in(HAS_ROLE).retain(role).hasNext();
	}

	@Override
	public boolean hasUser(User user) {
		return in(HAS_USER).retain(user).hasNext();
	}

	@Override
	public TransformablePage<? extends User> getVisibleUsers(MeshAuthUser user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = in(HAS_USER);
		return new DynamicTransformablePageImpl<User>(user, traversal, pagingInfo, READ_PERM, UserImpl.class);
	}

	@Override
	public TransformablePage<? extends Role> getRoles(User user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = in(HAS_ROLE);
		return new DynamicTransformablePageImpl<Role>(user, traversal, pagingInfo, READ_PERM, RoleImpl.class);
	}

	@Override
	public GroupResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		GroupResponse restGroup = new GroupResponse();
		restGroup.setName(getName());

		setRoles(ac, restGroup);
		fillCommonRestFields(ac, restGroup);
		setRolePermissions(ac, restGroup);

		return restGroup;
	}

	/**
	 * Load the roles that are assigned to this group and add the transformed references to the rest model.
	 * 
	 * @param ac
	 * @param restGroup
	 */
	private void setRoles(InternalActionContext ac, GroupResponse restGroup) {
		for (Role role : getRoles()) {
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(role.transformToReference());
			}
		}
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO don't allow deletion of the admin group
		batch.delete(this, true);
		getElement().remove();
		PermissionStore.invalidate();
	}

	@Override
	public Group update(InternalActionContext ac, SearchQueueBatch batch) {
		BootstrapInitializer boot = MeshInternal.get().boot();
		GroupUpdateRequest requestModel = ac.fromJson(GroupUpdateRequest.class);

		if (isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		if (shouldUpdate(requestModel.getName(), getName())) {
			Group groupWithSameName = boot.groupRoot().findByName(requestModel.getName());
			if (groupWithSameName != null && !groupWithSameName.getUuid().equals(getUuid())) {
				throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
			}

			setName(requestModel.getName());
			batch.store(this, true);
		}
		return this;
	}

	@Override
	public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		if (recursive) {
			for (User user : getUsers()) {
				user.applyPermissions(role, false, permissionsToGrant, permissionsToRevoke);
			}
		}
		super.applyPermissions(role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public void handleRelatedEntries(HandleElementAction action) {
		for (User user : getUsers()) {
			// We need to store users as well since users list their groups -
			// See {@link UserTransformer#toDocument(User)}
			action.call(user, null);
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
		return "/api/v1/groups/" + getUuid();
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
	public Single<GroupResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return db.operateTx(() -> {
			return Single.just(transformToRestSync(ac, level, languageTags));
		});
	}

}
