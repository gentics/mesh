package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static com.gentics.mesh.madl.type.EdgeTypeDefinition.edgeType;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * @see GroupRoot
 */
public class GroupRootImpl extends AbstractRootVertex<Group> implements GroupRoot {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(GroupRootImpl.class, MeshVertexImpl.class);
		type.createType(edgeType(HAS_GROUP));
		index.createIndex(edgeIndex(HAS_GROUP).withInOut().withOut());
	}

	@Override
	public Class<? extends Group> getPersistanceClass() {
		return GroupImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_GROUP;
	}

	@Override
	public void addGroup(Group group) {
		addItem(group);
	}

	@Override
	public void removeGroup(Group group) {
		removeItem(group);
	}

	@Override
	public Group create(String name, User creator, String uuid) {
		GroupImpl group = getGraph().addFramedVertex(GroupImpl.class);
		if (uuid != null) {
			group.setUuid(uuid);
		}
		group.setName(name);
		group.setCreated(creator);
		addGroup(group);

		return group;
	}

	@Override
	public void delete(BulkActionContext bac) {
		throw new NotImplementedException("The group root node can't be deleted");
	}

	@Override
	public Group create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		UserDaoWrapper userDao = mesh().boot().userDao();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}
		if (!userDao.hasPermission(requestUser, this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		MeshRoot root = mesh().boot().meshRoot();

		// Check whether a group with the same name already exists
		Group groupWithSameName = findByName(requestModel.getName());
		if (groupWithSameName != null && !groupWithSameName.getUuid().equals(getUuid())) {
			throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
		}

		// Finally create the group and set the permissions
		Group group = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, root.getGroupRoot(), group);
		batch.add(group.onCreated());
		return group;
	}

	@Override
	public void addUser(Group group, User user) {
		group.setUniqueLinkInTo(user, HAS_USER);

		// Add shortcut edge from user to roles of this group
		for (Role role : getRoles(group)) {
			user.setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
		}
	}

	@Override
	public void removeUser(Group group, User user) {
		group.unlinkIn(user, HAS_USER);

		// The user does no longer belong to the group so lets update the shortcut edges
		user.updateShortcutEdges();
		mesh().permissionCache().clear();
	}


	@Override
	public TraversalResult<? extends User> getUsers(Group group) {
		return group.in(HAS_USER, UserImpl.class);
	}

	@Override
	public TraversalResult<? extends Role> getRoles(Group group) {
		return group.in(HAS_ROLE, RoleImpl.class);
	}

	@Override
	public void addRole(Group group, Role role) {
		group.setUniqueLinkInTo(role, HAS_ROLE);

		// Add shortcut edges from role to users of this group
		for (User user : getUsers(group)) {
			user.setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
		}

	}

	@Override
	public void removeRole(Group group, Role role) {
		group.unlinkIn(role, HAS_ROLE);

		// Update the shortcut edges since the role does no longer belong to the group
		for (User user : getUsers(group)) {
			user.updateShortcutEdges();
		}
		mesh().permissionCache().clear();
	}

	@Override
	public boolean hasRole(Group group, Role role) {
		return group.in(HAS_ROLE).retain(role).hasNext();
	}

	@Override
	public boolean hasUser(Group group, User user) {
		return group.in(HAS_USER).retain(user).hasNext();
	}

	@Override
	public TransformablePage<? extends User> getVisibleUsers(Group group, MeshAuthUser user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = group.in(HAS_USER);
		return new DynamicTransformablePageImpl<User>(user, traversal, pagingInfo, READ_PERM, UserImpl.class);
	}

	@Override
	public TransformablePage<? extends Role> getRoles(Group group, User user, PagingParameters pagingInfo) {
		VertexTraversal<?, ?, ?> traversal = group.in(HAS_ROLE);
		return new DynamicTransformablePageImpl<Role>(user, traversal, pagingInfo, READ_PERM, RoleImpl.class);
	}


	@Override
	public GroupRoleAssignModel createRoleAssignmentEvent(Group group, Role role, Assignment assignment) {
		GroupRoleAssignModel model = new GroupRoleAssignModel();
		model.setGroup(group.transformToReference());
		model.setRole(role.transformToReference());
		switch (assignment) {
			case ASSIGNED:
				model.setEvent(GROUP_ROLE_ASSIGNED);
				break;
			case UNASSIGNED:
				model.setEvent(GROUP_ROLE_UNASSIGNED);
				break;
		}
		return model;
	}

	@Override
	public GroupUserAssignModel createUserAssignmentEvent(Group group, User user, Assignment assignment) {
		GroupUserAssignModel model = new GroupUserAssignModel();
		model.setGroup(group.transformToReference());
		model.setUser(user.transformToReference());
		switch (assignment) {
			case ASSIGNED:
				model.setEvent(GROUP_USER_ASSIGNED);
				break;
			case UNASSIGNED:
				model.setEvent(GROUP_USER_UNASSIGNED);
				break;
		}
		return model;
	}

	@Override
	public GroupResponse transformToRestSync(Group group, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		GroupResponse restGroup = new GroupResponse();
		if (fields.has("name")) {
			restGroup.setName(group.getName());
		}
		if (fields.has("roles")) {
			setRoles(group, ac, restGroup);
		}
		group.fillCommonRestFields(ac, fields, restGroup);

		setRolePermissions(group, ac, restGroup);
		return restGroup;
	}

	/**
	 * Load the roles that are assigned to this group and add the transformed references to the rest model.
	 *
	 * @param ac
	 * @param restGroup
	 */
	private void setRoles(Group group, InternalActionContext ac, GroupResponse restGroup) {
		for (Role role : getRoles(group)) {
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(role.transformToReference());
			}
		}
	}

	@Override
	public void delete(Group group, BulkActionContext bac) {
		// TODO don't allow deletion of the admin group
		bac.batch().add(group.onDeleted());

		Set<? extends User> affectedUsers = getUsers(group).stream().collect(Collectors.toSet());
		group.getElement().remove();
		for (User user : affectedUsers) {
			user.updateShortcutEdges();
			bac.add(user.onUpdated());
			bac.inc();
		}
		bac.process();
		mesh().permissionCache().clear();
	}

}
