package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isBlank;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.cache.NameCache;
import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainerEdge;
import com.gentics.mesh.core.data.NodeMigrationUser;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A persisting extension to {@link UserDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingUserDao extends UserDao, PersistingDaoGlobal<HibUser>, PersistingNamedEntityDao<HibUser> {

	Logger log = LoggerFactory.getLogger(PersistingUserDao.class);

	/**
	 * Update all shortcut edges.
	 */
	default void updateShortcutEdges(HibUser user) {
		// We don't expect any indexes or other shortcuts existing, so noop here.
	}

	/**
	 * Check the permission on the given element.
	 * 
	 * @param user
	 * @param element
	 * @param permission
	 * @return
	 */
	default boolean hasPermission(HibUser user, HibBaseElement element, InternalPermission permission) {
		if (log.isDebugEnabled()) {
			log.debug("Checking permissions for element {" + element.getUuid() + "}");
		}
		return hasPermissionForId(user, element.getId(), permission);
	}

	@Override
	default boolean hasPermissionForId(HibUser user, Object elementId, InternalPermission permission) {
		PermissionCache permissionCache = Tx.get().permissionCache();
		Boolean cached = permissionCache.hasPermission(user.getId(), permission, elementId);
		if (cached != null) {
			if (!cached && permission == READ_PUBLISHED_PERM) {
				return hasPermissionForId(user, elementId, READ_PERM);
			}

			return cached.booleanValue();
		} else {
			// Admin users have all permissions
			if (user.isAdmin()) {
				permissionCache.store(user.getId(), EnumSet.allOf(InternalPermission.class), elementId);
				return true;
			}

			EnumSet<InternalPermission> permissions = getPermissionsForElementId(user, elementId);
			permissionCache.store(user.getId(), permissions, elementId);
			if (permissions.contains(permission)) {
				return true;
			}

			// Fall back to read and check whether the user has read perm. Read permission also includes read published.
			if (permission == READ_PUBLISHED_PERM) {
				return hasPermissionForId(user, elementId, READ_PERM);
			} else {
				return false;
			}
		}
	}

	/**
	 * Get the permissions set on the element with given ID
	 * @param user user
	 * @param elementId element ID
	 * @return enumset of granted permissions
	 */
	EnumSet<InternalPermission> getPermissionsForElementId(HibUser user, Object elementId);

	/**
	 * Prepare the permissions of the user on the elements with given IDs.
	 * Implementations may e.g. preload the permissions and put them into the cache.
	 * @param user user
	 * @param elementIds collection of element IDs
	 */
	void preparePermissionsForElementIds(HibUser user, Collection<Object> elementIds);

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	default HibUser create(String username, HibUser creator) {
		return create(username, creator, null);
	}

	/**
	 * Initialize the newly created user. 
	 * 
	 * @param user
	 * @param username
	 * @param creator
	 * @return
	 */
	private HibUser init(HibUser user, String username, HibUser creator) {
		HibUser conflicting = findByUsername(username);
		if (conflicting != null && !conflicting.getUuid().equals(user.getUuid())) {
			throw conflict(conflicting.getUuid(), username, "user_conflicting_username");
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
		return user;
	}
	
	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	default HibUser create(String username, HibUser creator, String uuid) {
		HibUser user = createPersisted(uuid, u -> {
			init(u, username, creator);
		});		
		addBatchEvent(user.onCreated());
		uncacheSync(user);
		return mergeIntoPersisted(user);
	}

	/**
	 * Check whether the user is allowed to read the given node. Internally this check the currently configured version scope and check for
	 * {@link InternalPermission#READ_PERM} or {@link InternalPermission#READ_PUBLISHED_PERM}.
	 *
	 * @param user
	 * @param ac
	 * @param node
	 * @return
	 */
	default boolean canReadNode(HibUser user, InternalActionContext ac, HibNode node) {
		if (user instanceof NodeMigrationUser) {
			return true;
		}
		String version = ac.getVersioningParameters().getVersion();
		if (ContainerType.forVersion(version) == ContainerType.PUBLISHED) {
			return hasPermission(ac.getUser(), node, InternalPermission.READ_PUBLISHED_PERM);
		} else {
			return hasPermission(ac.getUser(), node, InternalPermission.READ_PERM);
		}
	}

	/**
	 * Check the read permission on the given container and fail if the needed permission to read the container is not set. This method will not fail if the
	 * user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	default void failOnNoReadPermission(HibUser user, HibNodeFieldContainer container, String branchUuid, String requestedVersion) {
		ContentDao contentDao = Tx.get().contentDao();
		HibNode node = contentDao.getNode(container);
		if (!hasReadPermission(user, container, branchUuid, requestedVersion)) {
			throw error(FORBIDDEN, "error_missing_perm", node.getUuid(),
				"published".equals(requestedVersion)
					? READ_PUBLISHED_PERM.getRestPerm().getName()
					: READ_PERM.getRestPerm().getName());
		}
	}

	/**
	 * This method will set CRUD permissions to the target node for all roles that would grant the given permission on the node. The method is most often used
	 * to assign CRUD permissions on newly created elements. Example for adding CRUD permissions on a newly created project: The method will first determine the
	 * list of roles that would initially enable you to create a new project. It will do so by examining the projectRoot node. After this step the CRUD
	 * permissions will be added to the newly created project and the found roles. In this case the call would look like this:
	 * addCRUDPermissionOnRole(projectRoot, Permission.CREATE_PERM, newlyCreatedProject); This method will ensure that all users/roles that would be able to
	 * create an element will also be able to CRUD it even when the creator of the element was only assigned to one of the enabling roles. Additionally the
	 * permissions of the source node are inherited by the target node. All permissions between the source node and roles are copied to the target node.
	 *
	 * @param user
	 * @param sourceNode
	 *            Node that will be checked against to find all roles that would grant the given permission.
	 * @param permission
	 *            Permission that is used in conjunction with the node to determine the list of affected roles.
	 * @param targetNode
	 *            Node to which the CRUD permissions will be assigned.
	 * @return Fluent API
	 */
	default HibUser addCRUDPermissionOnRole(HibUser user, HibBaseElement sourceNode, InternalPermission permission, HibBaseElement targetNode) {
		addPermissionsOnRole(user, sourceNode, permission, targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM, PUBLISH_PERM,
			READ_PUBLISHED_PERM);
		return user;
	}

	/**
	 * This method adds additional permissions to the target node. The roles are selected like in method
	 * {@link #addCRUDPermissionOnRole(HibUser, HasPermissions, InternalPermission, MeshVertex)} .
	 *
	 * @param user
	 * @param sourceNode
	 *            Node that will be checked
	 * @param permission
	 *            checked permission
	 * @param targetNode
	 *            target node
	 * @param toGrant
	 *            permissions to grant
	 * @return Fluent API
	 */
	default HibUser addPermissionsOnRole(HibUser user, HibBaseElement sourceNode, InternalPermission permission, HibBaseElement targetNode,
		InternalPermission... toGrant) {
		// TODO inject dao via DI
		RoleDao roleDao = Tx.get().roleDao();

		// 2. Add CRUD permission to identified roles and target node
		for (HibRole role : Tx.get().roleDao().getRolesWithPerm(sourceNode, permission)) {
			roleDao.grantPermissions(role, targetNode, toGrant);
		}
		return user;
	}

	/**
	 * Check if a node container has requested read permissions.
	 */
	default boolean hasReadPermission(HibUser user, HibNodeFieldContainer container, String branchUuid, String requestedVersion) {
		return Tx.get().contentDao().getContainerEdges(container).anyMatch(edge -> hasReadPermission(user, edge, branchUuid, requestedVersion));
	}

	/**
	 * Check if a node container edge has requested read permissions.
	 * 
	 * @param user
	 * @param edge
	 * @param branchUuid
	 * @param requestedVersion
	 * @return
	 */
	default boolean hasReadPermission(HibUser user, HibNodeFieldContainerEdge edge, String branchUuid, String requestedVersion) {
		PersistingContentDao contentDao = CommonTx.get().contentDao();
		HibNode node = edge.getNode();
		ContainerType type = ContainerType.forVersion(requestedVersion);

		if (ContainerType.PUBLISHED.equals(type)) {
			boolean published = contentDao.isType(edge, type, branchUuid);
			return published && hasPermission(user, node, READ_PUBLISHED_PERM);
		}
		return hasPermission(user, node, READ_PERM);
	}

	/**
	 * Check whether the given token code is valid. This method will also clear the token code if the token has expired.
	 *
	 * @parm user
	 * @param token
	 *            Token Code
	 * @param maxTokenAgeMins
	 *            maximum allowed token age in minutes
	 * @return
	 */
	default boolean isResetTokenValid(HibUser user, String token, int maxTokenAgeMins) {
		Long resetTokenIssueTimestamp = user.getResetTokenIssueTimestamp();
		if (token == null || resetTokenIssueTimestamp == null) {
			return false;
		}
		long maxTokenAge = 1000 * 60 * maxTokenAgeMins;
		long tokenAge = System.currentTimeMillis() - resetTokenIssueTimestamp;
		boolean isExpired = tokenAge > maxTokenAge;
		boolean isTokenMatch = token.equals(user.getResetToken());

		if (isTokenMatch && isExpired) {
			user.invalidateResetToken();
			return false;
		}
		return isTokenMatch && !isExpired;
	}

	/**
	 * Return the sub etag for the given user.
	 * 
	 * @param user
	 * @param ac
	 * @return
	 */
	default String getSubETag(HibUser user, InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(user.getLastEditedTimestamp());

		HibNode referencedNode = user.getReferencedNode();
		boolean expandReference = ac.getNodeParameters().getExpandedFieldnameList().contains("nodeReference")
			|| ac.getNodeParameters().getExpandAll();
		// We only need to compute the full etag if the referenced node is expanded.
		if (referencedNode != null && expandReference) {
			keyBuilder.append("-");
			keyBuilder.append(Tx.get().nodeDao().getETag(referencedNode, ac));
		} else if (referencedNode != null) {
			keyBuilder.append("-");
			keyBuilder.append(referencedNode.getUuid());
			keyBuilder.append(referencedNode.getProject().getName());
		}
		for (HibGroup group : getGroups(user)) {
			keyBuilder.append(group.getUuid());
		}
		keyBuilder.append(String.valueOf(user.isAdmin()));

		return keyBuilder.toString();
	}

	/**
	 * Return the permission info object for the given vertex.
	 *
	 * @param user
	 * @param element
	 * @return
	 */
	default PermissionInfo getPermissionInfo(HibUser user, HibBaseElement element) {
		PermissionInfo info = new PermissionInfo();
		Set<InternalPermission> permissions = getPermissions(user, element);
		for (InternalPermission perm : permissions) {
			info.set(perm.getRestPerm(), true);
		}
		info.setOthers(false, element.hasPublishPermissions());

		return info;
	}

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 *
	 * @param element
	 * @return
	 */
	default Set<InternalPermission> getPermissions(HibUser user, HibBaseElement element) {
		Predicate<? super InternalPermission> isValidPermission = perm -> perm != READ_PUBLISHED_PERM && perm != PUBLISH_PERM
			|| element.hasPublishPermissions();

		return Stream.of(InternalPermission.values())
			// Don't check for publish perms if it does not make sense for the vertex type
			.filter(isValidPermission)
			.filter(perm -> hasPermission(user, element, perm))
			.collect(Collectors.toSet());
	}

	@Override
	default UserResponse transformToRestSync(HibUser user, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		UserResponse restUser = new UserResponse();

		if (fields.has("username")) {
			restUser.setUsername(user.getUsername());
		}
		if (fields.has("emailAddress")) {
			restUser.setEmailAddress(user.getEmailAddress());
		}
		if (fields.has("firstname")) {
			restUser.setFirstname(user.getFirstname());
		}
		if (fields.has("lastname")) {
			restUser.setLastname(user.getLastname());
		}
		if (fields.has("admin")) {
			restUser.setAdmin(user.isAdmin());
		}
		if (fields.has("enabled")) {
			restUser.setEnabled(user.isEnabled());
		}
		if (fields.has("nodeReference")) {
			setNodeReference(user, ac, restUser, level);
		}
		if (fields.has("groups")) {
			setGroups(user, ac, restUser);
		}
		if (fields.has("rolesHash")) {
			restUser.setRolesHash(getRolesHash(user));
		}
		if (fields.has("forcedPasswordChange")) {
			restUser.setForcedPasswordChange(user.isForcedPasswordChange());
		}
		user.fillCommonRestFields(ac, fields, restUser);
		Tx.get().roleDao().setRolePermissions(user, ac, restUser);

		return restUser;
	}

	/**
	 * Add the node reference field to the user response (if required to).
	 *
	 * @param ac
	 * @param restUser
	 * @param level
	 *            Current depth level of transformation
	 */
	default void setNodeReference(HibUser user, InternalActionContext ac, UserResponse restUser, int level) {
		NodeParameters parameters = ac.getNodeParameters();

		// Check whether a node reference was set.
		HibNode node = user.getReferencedNode();
		if (node == null) {
			return;
		}

		// Check whether the node reference field of the user should be expanded
		boolean expandReference = parameters.getExpandedFieldnameList().contains("nodeReference") || parameters.getExpandAll();
		if (expandReference) {
			restUser.setNodeResponse(Tx.get().nodeDao().transformToRestSync(node, ac, level));
		} else {
			NodeReference userNodeReference = Tx.get().nodeDao().transformToReference(node, ac);
			restUser.setNodeReference(userNodeReference);
		}
	}

	/**
	 * Set the groups to which the user belongs in the rest model.
	 *
	 * @param ac
	 * @param restUser
	 */
	default void setGroups(HibUser user, InternalActionContext ac, UserResponse restUser) {
		// TODO filter by permissions
		for (HibGroup group : getGroups(user)) {
			GroupReference reference = group.transformToReference();
			restUser.getGroups().add(reference);
		}
	}

	@Override
	default boolean updateDry(HibUser user, InternalActionContext ac) {
		return update(user, ac, Tx.get().batch(), true);
	}

	@Override
	default boolean update(HibUser user, InternalActionContext ac, EventQueueBatch batch) {
		return update(user, ac, batch, false);
	}

	@Override
	default HibUser create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		Tx tx = Tx.get();
		HibBaseElement userRoot = tx.data().permissionRoots().user();
		GroupDao groupDao = tx.groupDao();
		ProjectDao projectDao = tx.projectDao();
		NodeDao nodeDao = tx.nodeDao();
		HibUser requestUser = ac.getUser();

		UserCreateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), UserCreateRequest.class);
		if (requestModel == null) {
			throw error(BAD_REQUEST, "error_parse_request_json_error");
		}
		if (isBlank(requestModel.getPassword())) {
			throw error(BAD_REQUEST, "user_missing_password");
		}
		if (isBlank(requestModel.getUsername())) {
			throw error(BAD_REQUEST, "user_missing_username");
		}
		if (!hasPermission(requestUser, userRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", userRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		String groupUuid = requestModel.getGroupUuid();
		HibUser user = create(requestModel.getUsername(), requestUser, uuid);
		user.setFirstname(requestModel.getFirstname());
		user.setUsername(requestModel.getUsername());
		user.setLastname(requestModel.getLastname());
		user.setEmailAddress(requestModel.getEmailAddress());
		updatePasswordHash(user, tx.passwordEncoder().encode(requestModel.getPassword()));
		Boolean forcedPasswordChange = requestModel.getForcedPasswordChange();
		if (forcedPasswordChange != null) {
			user.setForcedPasswordChange(forcedPasswordChange);
		}

		Boolean adminFlag = requestModel.getAdmin();
		if (adminFlag != null && adminFlag.booleanValue()) {
			if (requestUser.isAdmin()) {
				user.setAdmin(adminFlag);
			} else {
				throw error(FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
			}
		}

		inheritRolePermissions(requestUser, userRoot, user);
		ExpandableNode reference = requestModel.getNodeReference();

		if (!isBlank(groupUuid)) {
			HibGroup parentGroup = groupDao.loadObjectByUuid(ac, groupUuid, CREATE_PERM);
			groupDao.addUser(parentGroup, user);
			// batch.add(parentGroup.onUpdated());
			inheritRolePermissions(requestUser, parentGroup, user);
		}

		if (reference != null && reference instanceof NodeReference) {
			NodeReference basicReference = ((NodeReference) reference);
			String referencedNodeUuid = basicReference.getUuid();
			String projectName = basicReference.getProjectName();

			if (isBlank(projectName) || isBlank(referencedNodeUuid)) {
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
		return mergeIntoPersisted(user);
	}

	@Override
	default void delete(HibUser user) {
		// TODO unhardcode the admin name
		if ("admin".equals(user.getUsername())) {
			throw error(FORBIDDEN, "error_illegal_admin_deletion");
		}
		// disable();
		// TODO we should not really delete users. Instead we should remove
		// those from all groups and deactivate the access.
		// if (log.isDebugEnabled()) {
		// log.debug("Deleting user. The user will not be deleted. Instead the
		// user will be just disabled and removed from all groups.");
		// }
		// outE(HAS_USER).removeAll();
		CommonTx.get().batch().add(user.onDeleted());
		deletePersisted(user);
		CommonTx.get().data().maybeGetBulkActionContext().ifPresent(BulkActionContext::process);
		Tx.get().permissionCache().clear();
	}

	// TODO change this to an async call since hashing of the password is
	// blocking
	@Override
	default HibUser setPassword(HibUser user, String password) {
		if (isBlank(password)) {
			throw error(BAD_REQUEST, "user_missing_password");
		}
		String hashedPassword = Tx.get().passwordEncoder().encode(password);
		updatePasswordHash(user, hashedPassword);
		return mergeIntoPersisted(user);
	}

	@Override
	default void updatePasswordHash(HibUser user, String passwordHash) {
		user.setPasswordHash(passwordHash);
		// Password has changed, the user is not forced to change their password anymore.
		user.setForcedPasswordChange(false);
	}

	@Override
	default String getRolesHash(HibUser user) {
		return Stream.concat(
				Stream.of(user.isAdmin() ? "1" : "0"), 
				StreamSupport.stream(getRoles(user).spliterator(), false)
					.map(role -> role.getId().toString())
					.sorted())
			.collect(Collectors.joining());
	}

	private boolean update(HibUser user, InternalActionContext ac, EventQueueBatch batch, boolean dry) {
		UserUpdateRequest requestModel = ac.fromJson(UserUpdateRequest.class);
		boolean modified = false;
		if (shouldUpdate(requestModel.getUsername(), user.getUsername())) {
			HibUser conflictingUser = findByUsername(requestModel.getUsername());
			if (conflictingUser != null && !conflictingUser.getUuid().equals(user.getUuid())) {
				throw conflict(conflictingUser.getUuid(), requestModel.getUsername(), "user_conflicting_username");
			}
			if (!dry) {
				user.setUsername(requestModel.getUsername());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getAdmin(), user.isAdmin())) {
			if (ac.getUser().isAdmin()) {
				user.setAdmin(requestModel.getAdmin());
				// Permissions need to be purged
				Tx.get().permissionCache().clear();
			} else {
				throw error(FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getFirstname(), user.getFirstname())) {
			if (!dry) {
				user.setFirstname(requestModel.getFirstname());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getLastname(), user.getLastname())) {
			if (!dry) {
				user.setLastname(requestModel.getLastname());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getEmailAddress(), user.getEmailAddress())) {
			if (!dry) {
				user.setEmailAddress(requestModel.getEmailAddress());
			}
			modified = true;
		}

		if (shouldUpdate(requestModel.getForcedPasswordChange(), user.isForcedPasswordChange())) {
			if (!dry) {
				user.setForcedPasswordChange(requestModel.getForcedPasswordChange());
			}
			modified = true;
		}

		if (!isBlank(requestModel.getPassword())) {
			if (!dry) {
				updatePasswordHash(user, Tx.get().passwordEncoder().encode(requestModel.getPassword()));
			}
			modified = true;
		}

		if (requestModel.getNodeReference() != null) {
			ExpandableNode reference = requestModel.getNodeReference();
			String referencedNodeUuid = null;
			String projectName = null;
			if (reference instanceof NodeResponse) {
				NodeResponse response = (NodeResponse) reference;
				ProjectReference project = response.getProject();
				if (project == null) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				projectName = project.getName();
				if (isBlank(projectName)) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				referencedNodeUuid = response.getUuid();
			}
			if (reference instanceof NodeReference) {
				NodeReference basicReference = ((NodeReference) reference);
				if (isBlank(basicReference.getProjectName()) || isBlank(reference.getUuid())) {
					throw error(BAD_REQUEST, "user_incomplete_node_reference");
				}
				referencedNodeUuid = basicReference.getUuid();
				projectName = basicReference.getProjectName();
			}
			if (referencedNodeUuid != null && projectName != null) {
				/*
				 * TODO decide whether we need to check perms on the project as well
				 */
				HibProject project = Tx.get().projectDao().findByName(projectName);
				if (project == null) {
					throw error(BAD_REQUEST, "project_not_found", projectName);
				}
				HibNode node = Tx.get().nodeDao().loadObjectByUuid(project, ac, referencedNodeUuid, READ_PERM);
				if (!dry) {
					user.setReferencedNode(node);
				}
				modified = true;
			}
		}

		if (modified && !dry) {
			user.setEditor(ac.getUser());
			user.setLastEditedTimestamp();
			mergeIntoPersisted(user);
			batch.add(user.onUpdated());
		}
		return modified;
	}

	@Override
	default Optional<NameCache<HibUser>> maybeGetCache() {
		return Tx.maybeGet().map(CommonTx.class::cast).map(tx -> tx.data().mesh().userNameCache());
	}
}
