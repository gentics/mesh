package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_CREATOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_REFERENCE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getMeshSpringConfiguration;
import static com.gentics.mesh.util.VerticleHelper.loadObjectByUuidBlocking;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractIndexedVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UserImpl extends AbstractIndexedVertex<UserResponse>implements User {

	private static final Logger log = LoggerFactory.getLogger(UserImpl.class);

	public static final String FIRSTNAME_PROPERTY_KEY = "firstname";

	public static final String LASTNAME_PROPERTY_KEY = "lastname";

	public static final String USERNAME_PROPERTY_KEY = "username";

	public static final String EMAIL_PROPERTY_KEY = "emailAddress";

	public static final String PASSWORD_HASH_PROPERTY_KEY = "passwordHash";

	public static final String ENABLED_FLAG_PROPERTY_KEY = "enabledFlag";

	@Override
	public String getType() {
		return User.TYPE;
	}

	@Override
	public void disable() {
		setProperty(ENABLED_FLAG_PROPERTY_KEY, false);
	}

	// TODO do we really need disable and deactivate and remove?!
	@Override
	public void deactivate() {
		outE(HAS_GROUP).removeAll();
		disable();
	}

	@Override
	public void enable() {
		setProperty(ENABLED_FLAG_PROPERTY_KEY, true);
	}

	@Override
	public boolean isEnabled() {
		return BooleanUtils.toBoolean(getProperty(ENABLED_FLAG_PROPERTY_KEY).toString());
	}

	@Override
	public List<? extends GenericVertexImpl> getEditedElements() {
		return in(HAS_EDITOR).toList(GenericVertexImpl.class);
	}

	@Override
	public List<? extends GenericVertexImpl> getCreatedElements() {
		return in(HAS_CREATOR).toList(GenericVertexImpl.class);
	}

	@Override
	public String getFirstname() {
		return getProperty(FIRSTNAME_PROPERTY_KEY);
	}

	@Override
	public void setFirstname(String name) {
		setProperty(FIRSTNAME_PROPERTY_KEY, name);
	}

	@Override
	public String getLastname() {
		return getProperty(LASTNAME_PROPERTY_KEY);
	}

	@Override
	public void setLastname(String name) {
		setProperty(LASTNAME_PROPERTY_KEY, name);
	}

	@Override
	public String getName() {
		return getUsername();
	}

	@Override
	public String getUsername() {
		return getProperty(USERNAME_PROPERTY_KEY);
	}

	@Override
	public void setUsername(String name) {
		setProperty(USERNAME_PROPERTY_KEY, name);
	}

	@Override
	public void setName(String name) {
		setUsername(name);
	}

	@Override
	public String getEmailAddress() {
		return getProperty(EMAIL_PROPERTY_KEY);
	}

	@Override
	public void setEmailAddress(String emailAddress) {
		setProperty(EMAIL_PROPERTY_KEY, emailAddress);
	}

	/**
	 * Return all assigned groups.
	 */
	@Override
	public List<? extends Group> getGroups() {
		// TODO add permission handling?
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
		setLinkOutTo(node.getImpl(), HAS_NODE_REFERENCE);
	}

	@Override
	public String[] getPermissionNames(MeshVertex node) {
		Set<GraphPermission> permissions = getPermissions(node);
		String[] strings = new String[permissions.size()];
		Iterator<GraphPermission> it = permissions.iterator();
		for (int i = 0; i < permissions.size(); i++) {
			strings[i] = it.next().getSimpleName();
		}
		return strings;
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex node) {

		Set<GraphPermission> permissions = new HashSet<>();
		Set<? extends String> labels = out(HAS_USER).in(HAS_ROLE).outE(GraphPermission.labels()).mark().inV().retain(node.getImpl()).back().label()
				.toSet();
		for (String label : labels) {
			permissions.add(GraphPermission.valueOfLabel(label));
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(MeshVertex node, GraphPermission permission) {
		return out(HAS_USER).in(HAS_ROLE).outE(permission.label()).mark().inV().retain(node.getImpl()).hasNext();
	}

	@Override
	public User transformToRest(ActionContext ac, Handler<AsyncResult<UserResponse>> handler) {
		UserResponse restUser = new UserResponse();
		fillRest(restUser, ac);
		restUser.setUsername(getUsername());
		restUser.setEmailAddress(getEmailAddress());
		restUser.setFirstname(getFirstname());
		restUser.setLastname(getLastname());

		Node node = getReferencedNode();
		if (node != null) {
			boolean expandReference = ac.getExpandedFieldnames().contains("nodeReference");
			if (expandReference) {
				//				//TODO handle expanded form
				//				handler.handle(ac.failedFuture(BAD_REQUEST, "Expanding of node references not yet implemented."));
				//				return;
				node.transformToRest(ac, rh -> {
					restUser.setNodeReference(rh.result());
					for (Group group : getGroups()) {
						restUser.addGroup(group.getName());
					}
					handler.handle(Future.succeededFuture(restUser));
				});

			} else {
				NodeReferenceImpl userNodeReference = new NodeReferenceImpl();
				userNodeReference.setUuid(node.getUuid());
				if (node.getProject() != null) {
					userNodeReference.setProjectName(node.getProject().getName());
				} else {
					log.error("Project of node is null. Can't set project field of user nodeReference.");
					// TODO handle this case
				}
				restUser.setNodeReference(userNodeReference);
				for (Group group : getGroups()) {
					restUser.addGroup(group.getName());
				}
				handler.handle(Future.succeededFuture(restUser));

			}
		} else {
			for (Group group : getGroups()) {
				restUser.addGroup(group.getName());
			}
			handler.handle(Future.succeededFuture(restUser));
		}
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
		setLinkOutTo(group.getImpl(), HAS_USER);
	}

	@Override
	public String getPasswordHash() {
		return getProperty(PASSWORD_HASH_PROPERTY_KEY);
	}

	@Override
	public void setPasswordHash(String hash) {
		setProperty(PASSWORD_HASH_PROPERTY_KEY, hash);
	}

	@Override
	public void addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode) {

		// 1. Determine all roles that grant given permission on the source node.
		List<? extends Role> rolesThatGrantPermission = sourceNode.getImpl().in(permission.label()).has(RoleImpl.class)
				.toListExplicit(RoleImpl.class);

		// 2. Add CRUD permission to identified roles and target node
		for (Role role : rolesThatGrantPermission) {
			role.grantPermissions(targetNode, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);
		}

		inheritRolePermissions(sourceNode, targetNode);
	}

	@Override
	public void inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {

		for (GraphPermission perm : GraphPermission.values()) {
			List<? extends Role> rolesWithPerm = sourceNode.getImpl().in(perm.label()).has(RoleImpl.class).toListExplicit(RoleImpl.class);
			for (Role role : rolesWithPerm) {
				if (log.isDebugEnabled()) {
					log.debug("Granting permission {" + perm.name() + "} to node {" + targetNode.getUuid() + "} on role {" + role.getName() + "}");
				}
				role.grantPermissions(targetNode, perm);
			}
		}

	}

	@Override
	public void delete() {
		// TODO we should not really delete users. Instead we should remove those from all groups and deactivate the access.
		if (log.isDebugEnabled()) {
			log.debug("Deleting user. The user will not be deleted. Instead the user will be just disabled and removed from all groups.");
		}
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
	public void update(ActionContext ac, Handler<AsyncResult<Void>> handler) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		UserUpdateRequest requestModel;
		try {
			requestModel = JsonUtil.readNode(ac.getBodyAsString(), UserUpdateRequest.class, ServerSchemaStorage.getSchemaStorage());
			SearchQueueBatch batch = null;
			try (Trx txUpdate = db.trx()) {

				if (requestModel.getUsername() != null && !getUsername().equals(requestModel.getUsername())) {
					if (BootstrapInitializer.getBoot().userRoot().findByUsername(requestModel.getUsername()) != null) {
						handler.handle(ac.failedFuture(CONFLICT, "user_conflicting_username"));
						return;
					}
					setUsername(requestModel.getUsername());
				}

				if (!isEmpty(requestModel.getFirstname()) && !getFirstname().equals(requestModel.getFirstname())) {
					setFirstname(requestModel.getFirstname());
				}

				if (!isEmpty(requestModel.getLastname()) && !getLastname().equals(requestModel.getLastname())) {
					setLastname(requestModel.getLastname());
				}

				if (!isEmpty(requestModel.getEmailAddress()) && !getEmailAddress().equals(requestModel.getEmailAddress())) {
					setEmailAddress(requestModel.getEmailAddress());
				}

				if (!isEmpty(requestModel.getPassword())) {
					setPasswordHash(MeshSpringConfiguration.getMeshSpringConfiguration().passwordEncoder().encode(requestModel.getPassword()));
				}

				setEditor(ac.getUser());
				setLastEditedTimestamp(System.currentTimeMillis());
				if (requestModel.getNodeReference() != null) {
					NodeReference reference = requestModel.getNodeReference();
					//TODO also handle full node response inside node reference field
					if (reference instanceof NodeReferenceImpl) {
						NodeReferenceImpl basicReference = ((NodeReferenceImpl) reference);
						if (isEmpty(basicReference.getProjectName()) || isEmpty(reference.getUuid())) {
							handler.handle(ac.failedFuture(BAD_REQUEST, "user_incomplete_node_reference"));
							return;
						} else {
							String referencedNodeUuid = basicReference.getUuid();
							String projectName = basicReference.getProjectName();
							/* TODO decide whether we need to check perms on the project as well */
							Project project = BootstrapInitializer.getBoot().projectRoot().findByName(projectName);
							if (project == null) {
								handler.handle(ac.failedFuture(BAD_REQUEST, "project_not_found", projectName));
								return;
							} else {
								Node node = loadObjectByUuidBlocking(ac, referencedNodeUuid, READ_PERM, project.getNodeRoot());
								setReferencedNode(node);
								batch = addIndexBatch(UPDATE_ACTION);
								txUpdate.success();
							}
						}
					}
				} else {
					batch = addIndexBatch(UPDATE_ACTION);
					txUpdate.success();
				}
			}
			processOrFail2(ac, batch, handler);
		} catch (IOException e) {
			handler.handle(Future.failedFuture(e));
		}

	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		//		for (GenericVertex<?> element : getCreatedElements()) {
		//			batch.addEntry(element, UPDATE_ACTION);
		//		}
		//		for (GenericVertex<?> element : getEditedElements()) {
		//			batch.addEntry(element, UPDATE_ACTION);
		//		}
	}

}
