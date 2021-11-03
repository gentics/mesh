package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see UserDaoWrapper
 */
public class UserDaoWrapperImpl extends AbstractCoreDaoWrapper<UserResponse, HibUser, User> implements UserDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(UserDaoWrapperImpl.class);

	@Inject
	public UserDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions) {
		super(boot, permissions);
	}

	@Override
	public boolean hasPermission(HibUser user, HibBaseElement element, InternalPermission permission) {
		if (log.isTraceEnabled()) {
			log.debug("Checking permissions for element {" + element.getUuid() + "}");
		}
		return hasPermissionForId(user, element.getId(), permission);
	}

	@Override
	public boolean hasPermissionForId(HibUser user, Object elementId, InternalPermission permission) {
		PermissionCache permissionCache = Tx.get().permissionCache();
		if (permissionCache.hasPermission(user.getId(), permission, elementId)) {
			return true;
		} else {
			// Admin users have all permissions
			if (user.isAdmin()) {
				for (InternalPermission perm : InternalPermission.values()) {
					permissionCache.store(user.getId(), perm, elementId);
				}
				return true;
			}

			FramedGraph graph = GraphDBTx.getGraphTx().getGraph();
			// Find all roles that are assigned to the user by checking the
			// shortcut edge from the index
			String idxKey = "e." + ASSIGNED_TO_ROLE + "_out";
			Iterable<Edge> roleEdges = graph.getEdges(idxKey.toLowerCase(), user.getId());
			Vertex vertex = graph.getVertex(elementId);
			for (Edge roleEdge : roleEdges) {
				Vertex role = roleEdge.getVertex(Direction.IN);

				Set<String> allowedRoles = vertex.getProperty(permission.propertyKey());
				boolean hasPermission = allowedRoles != null && allowedRoles.contains(role.<String>getProperty("uuid"));
				if (hasPermission) {
					// We only store granting permissions in the store in order
					// reduce the invalidation calls.
					// This way we do not need to invalidate the cache if a role
					// is removed from a group or a role is deleted.
					permissionCache.store(user.getId(), permission, elementId);
					return true;
				}
			}
			// Fall back to read and check whether the user has read perm. Read permission also includes read published.
			if (permission == READ_PUBLISHED_PERM) {
				return hasPermissionForId(user, elementId, READ_PERM);
			} else {
				return false;
			}
		}

	}

	@Override
	public Result<? extends User> findAll() {
		return getRoot().findAll();
	}

	@Override
	public Page<? extends User> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return getRoot().findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibUser> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibUser> extraFilter) {
		return boot.get().meshRoot().getUserRoot().findAllWrapped(ac, pagingInfo, extraFilter);
	}

	@Override
	public User findByName(String name) {
		return getRoot().findByName(name);
	}

	@Override
	public HibUser findByUsername(String username) {
		return boot.get().meshRoot().getUserRoot().findByUsername(username);
	}

	@Override
	public HibUser findByUuid(String uuid) {
		return getRoot().findByUuid(uuid);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return boot.get().meshRoot().getUserRoot().findMeshAuthUserByUsername(username);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		return boot.get().meshRoot().getUserRoot().findMeshAuthUserByUuid(userUuid);
	}

	@Override
	public HibUser inheritRolePermissions(HibUser user, HibBaseElement source, HibBaseElement target) {
		for (InternalPermission perm : InternalPermission.values()) {
			String key = perm.propertyKey();
			toGraph(target).property(key, toGraph(source).property(key));
		}
		return user;
	}

	@Override
	public HibUser addGroup(HibUser user, HibGroup group) {
		User graphUser = toGraph(user);
		Group graphGroup = toGraph(group);
		graphUser.addGroup(graphGroup);
		return user;
	}

	/**
	 * Return the global amount of users stored in mesh.
	 */
	public long count() {
		return getRoot().globalCount();
	}

	@Override
	public String getETag(HibUser user, InternalActionContext ac) {
		User graphUser = toGraph(user);
		return graphUser.getETag(ac);
	}

	@Override
	public Result<? extends HibGroup> getGroups(HibUser user) {
		User graphUser = toGraph(user);
		return graphUser.getGroups();
	}

	@Override
	public Iterable<? extends HibRole> getRoles(HibUser user) {
		User graphUser = toGraph(user);
		return graphUser.getRoles();
	}

	@Override
	public Page<? extends HibRole> getRolesViaShortcut(HibUser fromUser, HibUser authUser, PagingParameters pagingInfo) {
		return toGraph(fromUser).getRolesViaShortcut(authUser, pagingInfo);
	}

	@Override
	public Page<? extends HibGroup> getGroups(HibUser fromUser, HibUser authUser, PagingParameters pagingInfo) {
		return toGraph(fromUser).getGroups(authUser, pagingInfo);
	}

	@Override
	protected RootVertex<User> getRoot() {
		return boot.get().meshRoot().getUserRoot();
	}

	@Override
	public String getRolesHash(HibUser user) {
		return toGraph(user).getRolesHash();
	}

	@Override
	public void updateShortcutEdges(HibUser user) {
		GroupDao groupRoot = Tx.get().groupDao();
		User graph = toGraph(user);
		graph.outE(ASSIGNED_TO_ROLE).removeAll();
		for (HibGroup group : graph.getGroups()) {
			for (HibRole role : groupRoot.getRoles(group)) {
				graph.setUniqueLinkOutTo(toGraph(role), ASSIGNED_TO_ROLE);
			}
		}
	}
}
