package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

import rx.Completable;
import rx.Single;

/**
 * @see Group
 */
public class GroupImpl extends AbstractMeshCoreVertex<GroupResponse, Group> implements Group {

	public static final String NAME_KEY = "name";

	public static void checkIndices(Database database) {
		database.addVertexType(GroupImpl.class, MeshVertexImpl.class);
	}

	public GroupReference createEmptyReferenceModel() {
		return new GroupReference();
	}

	@Override
	public String getType() {
		return Group.TYPE;
	}

	public String getName() {
		return getProperty(NAME_KEY);
	}

	public void setName(String name) {
		setProperty(NAME_KEY, name);
	}

	public List<? extends User> getUsers() {
		return in(HAS_USER).has(UserImpl.class).toListExplicit(UserImpl.class);
	}

	public void addUser(User user) {
		setUniqueLinkInTo(user.getImpl(), HAS_USER);

		// Add shortcut edge from user to roles of this group
		for (Role role : getRoles()) {
			user.getImpl().setUniqueLinkOutTo(role.getImpl(), ASSIGNED_TO_ROLE);
		}
	}

	public void removeUser(User user) {
		unlinkIn(user.getImpl(), HAS_USER);

		// Remove shortcut edge from user to roles of this group
		for (Role role : getRoles()) {
			user.getImpl().unlinkOut(role.getImpl(), ASSIGNED_TO_ROLE);
		}
	}

	public List<? extends Role> getRoles() {
		return in(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	public void addRole(Role role) {
		setUniqueLinkInTo(role.getImpl(), HAS_ROLE);

		// Add shortcut edges from role to users of this group
		for (User user : getUsers()) {
			user.getImpl().setUniqueLinkOutTo(role.getImpl(), ASSIGNED_TO_ROLE);
		}

	}

	public void removeRole(Role role) {
		unlinkIn(role.getImpl(), HAS_ROLE);

		// Remove shortcut edges from role to users of this group
		for (User user : getUsers()) {
			user.getImpl().unlinkOut(role.getImpl(), ASSIGNED_TO_ROLE);
		}

	}

	// TODO add java handler
	public boolean hasRole(Role role) {
		return in(HAS_ROLE).retain(role.getImpl()).hasNext();
	}

	public boolean hasUser(User user) {
		return in(HAS_USER).retain(user.getImpl()).hasNext();
	}

	/**
	 * Get all users within this group that are visible for the given user.
	 */
	public PageImpl<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingParameters pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_USER).mark().in(GraphPermission.READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back().has(UserImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_USER).mark().in(GraphPermission.READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back().has(UserImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, UserImpl.class);
	}

	@Override
	public PageImpl<? extends Role> getRoles(MeshAuthUser requestUser, PagingParameters pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_ROLE);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_ROLE);

		PageImpl<? extends Role> page = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
		return page;

	}

	@Override
	public Single<GroupResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {

		GroupResponse restGroup = new GroupResponse();
		restGroup.setName(getName());

		// for (User user : group.getUsers()) {
		// String name = user.getUsername();
		// if (name != null) {
		// restGroup.getUsers().add(name);
		// }
		// Collections.sort(restGroup.getUsers());

		for (Role role : getRoles()) {
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(role.transformToReference());
			}
		}

		// // Set<Group> children = groupRepository.findChildren(group);
		// Set<Group> children = group.getGroups();
		// for (Group childGroup : children) {
		// restGroup.getGroups().add(childGroup.getName());
		// }

		// Add common fields
		Completable fillCommonFields = fillCommonRestFields(ac, restGroup);

		// Role permissions
		Completable setRoles = setRolePermissions(ac, restGroup);

		// Merge and complete
		return Completable.merge(setRoles, fillCommonFields).andThen(Single.just(restGroup));
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO don't allow deletion of the admin group
		batch.addEntry(this, DELETE_ACTION);
		addRelatedEntries(batch, DELETE_ACTION);
		getElement().remove();
	}

	@Override
	public Single<? extends Group> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		return db.noTrx(() -> {
			GroupUpdateRequest requestModel = ac.fromJson(GroupUpdateRequest.class);

			if (isEmpty(requestModel.getName())) {
				throw error(BAD_REQUEST, "error_name_must_be_set");
			}

			if (shouldUpdate(requestModel.getName(), getName())) {
				Group groupWithSameName = boot.groupRoot().findByName(requestModel.getName()).toBlocking().value();
				if (groupWithSameName != null && !groupWithSameName.getUuid().equals(getUuid())) {
					throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
				}

				return db.trx(() -> {
					setName(requestModel.getName());
					return createIndexBatch(STORE_ACTION);
				}).process().toSingleDefault(this);

			} else {
				return Single.just(this);
			}
		});

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
	public GroupImpl getImpl() {
		return this;
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		for (User user : getUsers()) {
			batch.addEntry(user, STORE_ACTION);
		}
	}

}
