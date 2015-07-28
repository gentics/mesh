package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_NODE_REFERENCE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;
import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getMeshSpringConfiguration;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuid;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Configurable;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;

public class UserImpl extends AbstractGenericVertex<UserResponse> implements User {

	public static final String FIRSTNAME_KEY = "firstname";

	public static final String LASTNAME_KEY = "lastname";

	public static final String USERNAME_KEY = "username";

	public static final String EMAIL_KEY = "emailAddress";

	public static final String PASSWORD_HASH_KEY = "passwordHash";

	public static final String ENABLED_FLAG = "enabledFlag";

	@Override
	public void disable() {
		setProperty(ENABLED_FLAG, false);
	}

	@Override
	public void deactivate() {
		outE(HAS_GROUP).removeAll();
		disable();
	}

	@Override
	public void enable() {
		setProperty(ENABLED_FLAG, true);
	}

	@Override
	public boolean isEnabled() {
		return BooleanUtils.toBoolean(getProperty(ENABLED_FLAG).toString());
	}

	@Override
	public String getFirstname() {
		return getProperty(FIRSTNAME_KEY);
	}

	@Override
	public void setFirstname(String name) {
		setProperty(FIRSTNAME_KEY, name);
	}

	@Override
	public String getLastname() {
		return getProperty(LASTNAME_KEY);
	}

	@Override
	public void setLastname(String name) {
		setProperty(LASTNAME_KEY, name);
	}

	@Override
	public String getName() {
		return getUsername();
	}

	// TODO add unique index
	@Override
	public String getUsername() {
		return getProperty(USERNAME_KEY);
	}

	@Override
	public void setUsername(String name) {
		setProperty(USERNAME_KEY, name);
	}

	@Override
	public void setName(String name) {
		setUsername(name);
	}

	@Override
	public String getEmailAddress() {
		return getProperty(EMAIL_KEY);
	}

	@Override
	public void setEmailAddress(String emailAddress) {
		setProperty(EMAIL_KEY, emailAddress);
	}

	/**
	 * Return all assigned groups.
	 */
	@Override
	public List<? extends Group> getGroups() {
		//TODO add permission handling?
		return out(HAS_USER).has(GroupImpl.class).toListExplicit(GroupImpl.class);
	}

	@Override
	public List<? extends Role> getRoles() {
		return out(HAS_GROUP).out(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	@Override
	public Node getReferencedNode() {
		return out(HAS_NODE_REFERENCE).has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public void setReferencedNode(Node node) {
		setLinkOut(node.getImpl(), HAS_NODE_REFERENCE);
	}

	@Override
	public String[] getPermissionNames(MeshVertex node) {
		Set<Permission> permissions = getPermissions(node);
		String[] strings = new String[permissions.size()];
		Iterator<Permission> it = permissions.iterator();
		for (int i = 0; i < permissions.size(); i++) {
			strings[i] = it.next().getHumanName();
		}
		return strings;
	}

	@Override
	public Set<Permission> getPermissions(MeshVertex node) {

		Set<Permission> permissions = new HashSet<>();
		Set<? extends String> labels = out(HAS_USER).in(HAS_ROLE).outE(Permission.labels()).mark().inV().retain(node.getImpl()).back().label()
				.toSet();
		for (String label : labels) {
			permissions.add(Permission.valueOfLabel(label));
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(MeshVertex node, Permission permission) {
		return out(HAS_USER).in(HAS_ROLE).outE(permission.label()).mark().inV().retain(node.getImpl()).hasNext();
	}

	@Override
	public User transformToRest(RoutingContext rc, Handler<AsyncResult<UserResponse>> handler) {
		UserResponse restUser = new UserResponse();
		fillRest(restUser, rc);
		restUser.setUsername(getUsername());
		restUser.setEmailAddress(getEmailAddress());
		restUser.setFirstname(getFirstname());
		restUser.setLastname(getLastname());

		Node node = getReferencedNode();
		if (node != null) {
			NodeReference userNodeReference = new NodeReference();
			userNodeReference.setUuid(node.getUuid());
			if (node.getProject() != null) {
				userNodeReference.setProjectName(node.getProject().getName());
			} else {
				//TODO handle this case
			}
			restUser.setNodeReference(userNodeReference);
		}
		for (Group group : getGroups()) {
			restUser.addGroup(group.getName());
		}
		handler.handle(Future.succeededFuture(restUser));
		return this;
	}

	@Override
	public UserReference transformToUserReference() {
		UserReference reference = new UserReference();
		reference.setName(getUsername());
		reference.setUuid(getUuid());
		return reference;
	}

	@Override
	public void addGroup(Group group) {
		linkOut(group.getImpl(), HAS_USER);
	}

	@Override
	public long getGroupCount() {
		return out(HAS_USER).has(GroupImpl.class).count();
	}

	@Override
	public String getPasswordHash() {
		return getProperty(PASSWORD_HASH_KEY);
	}

	@Override
	public void setPasswordHash(String hash) {
		setProperty(PASSWORD_HASH_KEY, hash);
	}

	@Override
	public void addCRUDPermissionOnRole(MeshVertex node, Permission permission, MeshVertex targetNode) {

		// 1. Determine all roles that grant given permission
		List<? extends Role> rolesThatGrantPermission = node.getImpl().in(permission.label()).has(RoleImpl.class).toListExplicit(RoleImpl.class);

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : rolesThatGrantPermission) {
			role.addPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}
	}

	@Override
	public void delete() {
		// TODO we should not really delete users. Instead we should remove those from all groups and deactivate the access.
		outE(HAS_USER).removeAll();
		disable();
	}

	/**
	 * Encode the given password and set the generated hash.
	 * 
	 * @param password
	 */
	@Override
	public void setPassword(String password) {
		setPasswordHash(getMeshSpringConfiguration().passwordEncoder().encode(password));
	}

	@Override
	public UserImpl getImpl() {
		return this;
	}

	@Override
	public void fillUpdateFromRest(RoutingContext rc, UserUpdateRequest requestModel, Handler<AsyncResult<User>> handler) {
		I18NService i18n = I18NService.getI18n();

		if (requestModel.getUsername() != null && getUsername() != requestModel.getUsername()) {
			if (BootstrapInitializer.getBoot().userRoot().findByUsername(requestModel.getUsername()) != null) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "user_conflicting_username"))));
				return;
			}
			setUsername(requestModel.getUsername());
		}

		if (!isEmpty(requestModel.getFirstname()) && getFirstname() != requestModel.getFirstname()) {
			setFirstname(requestModel.getFirstname());
		}

		if (!isEmpty(requestModel.getLastname()) && getLastname() != requestModel.getLastname()) {
			setLastname(requestModel.getLastname());
		}

		if (!isEmpty(requestModel.getEmailAddress()) && getEmailAddress() != requestModel.getEmailAddress()) {
			setEmailAddress(requestModel.getEmailAddress());
		}

		if (!isEmpty(requestModel.getPassword())) {
			setPasswordHash(MeshSpringConfiguration.getMeshSpringConfiguration().passwordEncoder().encode(requestModel.getPassword()));
		}

		if (requestModel.getNodeReference() != null) {
			NodeReference reference = requestModel.getNodeReference();
			if (isEmpty(reference.getProjectName()) || isEmpty(reference.getUuid())) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_incomplete_node_reference"))));
				return;
			} else {
				String referencedNodeUuid = requestModel.getNodeReference().getUuid();
				String projectName = requestModel.getNodeReference().getProjectName();
				/* TODO decide whether we need to check perms on the project as well */
				Project project = BootstrapInitializer.getBoot().projectRoot().findByName(projectName);
				if (project == null) {
					handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "project_not_found", projectName))));
				} else {
					loadObjectByUuid(rc, referencedNodeUuid, READ_PERM, project.getNodeRoot(), nrh -> {
						if (hasSucceeded(rc, nrh)) {
							setReferencedNode(nrh.result());
							handler.handle(Future.succeededFuture(this));
						}
					});
				}
			}
		} else {
			handler.handle(Future.succeededFuture(this));
		}

	}

	@Override
	public void fillCreateFromRest(RoutingContext rc, UserCreateRequest requestModel, Group parentGroup, Handler<AsyncResult<User>> handler) {
		I18NService i18n = I18NService.getI18n();

		setFirstname(requestModel.getFirstname());
		setUsername(requestModel.getUsername());
		setLastname(requestModel.getLastname());
		setEmailAddress(requestModel.getEmailAddress());
		setPasswordHash(MeshSpringConfiguration.getMeshSpringConfiguration().passwordEncoder().encode(requestModel.getPassword()));
		addGroup(parentGroup);
		MeshAuthUser requestUser = getUser(rc);
		requestUser.addCRUDPermissionOnRole(parentGroup, CREATE_PERM, this);
		NodeReference reference = requestModel.getNodeReference();
		if (reference != null) {
			if (isEmpty(reference.getProjectName()) || isEmpty(reference.getUuid())) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "user_incomplete_node_reference"))));
			} else {
				String referencedNodeUuid = requestModel.getNodeReference().getUuid();
				String projectName = requestModel.getNodeReference().getProjectName();
				/* TODO decide whether we need to check perms on the project as well */
				Project project = BootstrapInitializer.getBoot().projectRoot().findByName(projectName);
				if (project == null) {
					handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "project_not_found", projectName))));
				} else {
					loadObjectByUuid(rc, referencedNodeUuid, READ_PERM, project.getNodeRoot(), nrh -> {
						if (hasSucceeded(rc, nrh)) {
							setReferencedNode(nrh.result());
							handler.handle(Future.succeededFuture(this));
						}
					});
				}
			}
		} else {
			handler.handle(Future.succeededFuture(this));
		}

	}

}
