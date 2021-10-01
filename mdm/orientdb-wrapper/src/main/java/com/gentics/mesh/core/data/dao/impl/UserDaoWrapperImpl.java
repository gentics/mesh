package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
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
public class UserDaoWrapperImpl extends AbstractDaoWrapper<HibUser> implements UserDaoWrapper {

	private static final Logger log = LoggerFactory.getLogger(UserDaoWrapperImpl.class);

	private final PasswordEncoder passwordEncoder;

	@Inject
	public UserDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, PasswordEncoder passwordEncoder) {
		super(boot, permissions);
		this.passwordEncoder = passwordEncoder;
	}

	@Override
	public User create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		GroupDao groupDao = boot.get().groupDao();
		ProjectDao projectDao = boot.get().projectDao();
		NodeDao nodeDao = boot.get().nodeDao();
		HibUser requestUser = ac.getUser();

		UserCreateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), UserCreateRequest.class);
		if (requestModel == null) {
			throw error(BAD_REQUEST, "error_parse_request_json_error");
		}
		if (isEmpty(requestModel.getPassword())) {
			throw error(BAD_REQUEST, "user_missing_password");
		}
		if (isEmpty(requestModel.getUsername())) {
			throw error(BAD_REQUEST, "user_missing_username");
		}
		if (!hasPermission(requestUser, userRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", userRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		String groupUuid = requestModel.getGroupUuid();
		String userName = requestModel.getUsername();
		HibUser conflictingUser = findByUsername(userName);
		if (conflictingUser != null) {
			throw conflict(conflictingUser.getUuid(), userName, "user_conflicting_username");
		}

		User user = create(requestModel.getUsername(), requestUser, uuid);
		user.setFirstname(requestModel.getFirstname());
		user.setUsername(requestModel.getUsername());
		user.setLastname(requestModel.getLastname());
		user.setEmailAddress(requestModel.getEmailAddress());
		user.setPasswordHash(passwordEncoder.encode(requestModel.getPassword()));
		Boolean forcedPasswordChange = requestModel.getForcedPasswordChange();
		if (forcedPasswordChange != null) {
			user.setForcedPasswordChange(forcedPasswordChange);
		}

		Boolean adminFlag = requestModel.getAdmin();
		if (adminFlag != null) {
			if (requestUser.isAdmin()) {
				user.setAdmin(adminFlag);
			} else {
				throw error(FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
			}
		}

		inheritRolePermissions(requestUser, userRoot, user);
		ExpandableNode reference = requestModel.getNodeReference();
		batch.add(user.onCreated());

		if (!isEmpty(groupUuid)) {
			HibGroup parentGroup = groupDao.loadObjectByUuid(ac, groupUuid, CREATE_PERM);
			groupDao.addUser(parentGroup, user);
			// batch.add(parentGroup.onUpdated());
			inheritRolePermissions(requestUser, parentGroup, user);
		}

		if (reference != null && reference instanceof NodeReference) {
			NodeReference basicReference = ((NodeReference) reference);
			String referencedNodeUuid = basicReference.getUuid();
			String projectName = basicReference.getProjectName();

			if (isEmpty(projectName) || isEmpty(referencedNodeUuid)) {
				throw error(BAD_REQUEST, "user_incomplete_node_reference");
			}

			// TODO decide whether we need to check perms on the project as well
			HibProject project = projectDao.findByName(projectName);
			if (project == null) {
				throw error(BAD_REQUEST, "project_not_found", projectName);
			}
			HibNode node = nodeDao.loadObjectByUuid(project, ac, referencedNodeUuid, READ_PERM);
			user.setReferencedNode(node);
		} else if (reference != null) {
			// TODO handle user create using full node rest model.
			throw error(BAD_REQUEST, "user_creation_full_node_reference_not_implemented");
		}
		return user;
	}

	/**
	 * Encode the given password and set the generated hash.
	 *
	 * @param password
	 *            Plain password to be hashed and set
	 * @return Fluent API
	 */
	@Override
	public HibUser setPassword(HibUser user, String password) {
		user.setPasswordHash(passwordEncoder.encode(password));
		return user;
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
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findAll();
	}

	@Override
	public Page<? extends User> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibUser> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibUser> extraFilter) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findAllWrapped(ac, pagingInfo, extraFilter);
	}

	@Override
	public User findByName(String name) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findByName(name);
	}

	@Override
	public HibUser findByUsername(String username) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findByUsername(username);
	}

	@Override
	public HibUser findByUuid(String uuid) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findByUuid(uuid);
	}

	@Override
	public HibUser loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public HibUser loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findMeshAuthUserByUsername(username);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		return userRoot.findMeshAuthUserByUuid(userUuid);
	}

	@Override
	public void delete(HibUser user, BulkActionContext bac) {
		// TODO don't allow this for the admin user
		// disable();
		// TODO we should not really delete users. Instead we should remove
		// those from all groups and deactivate the access.
		// if (log.isDebugEnabled()) {
		// log.debug("Deleting user. The user will not be deleted. Instead the
		// user will be just disabled and removed from all groups.");
		// }
		// outE(HAS_USER).removeAll();
		bac.add(user.onDeleted());
		user.remove();
		bac.process();
		Tx.get().permissionCache().clear();
	}

	@Override
	public User create(String username, HibUser creator, String uuid) {
		UserRoot userRoot = boot.get().meshRoot().getUserRoot();
		User user = userRoot.create();
		if (uuid != null) {
			user.setUuid(uuid);
		}
		user.setUsername(username);
		user.enable();
		user.generateBucketId();

		if (creator != null) {
			user.setCreator(creator);
			user.setCreationTimestamp();
			user.setEditor(creator);
			user.setLastEditedTimestamp();
		}
		userRoot.addItem(user);
		return user;
	}

	@Override
	public HibUser inheritRolePermissions(HibUser user, HibBaseElement source, HibBaseElement target) {
		for (InternalPermission perm : InternalPermission.values()) {
			String key = perm.propertyKey();
			((MeshVertex) target).property(key, ((MeshVertex) source).property(key));
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
		return boot.get().meshRoot().getUserRoot().globalCount();
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

}
