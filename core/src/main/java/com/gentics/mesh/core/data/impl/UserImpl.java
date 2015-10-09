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
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.error;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getInstance;
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
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

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
	//TODO migrate to non blocking api
	public String[] getPermissionNames(InternalActionContext ac, MeshVertex node) {
		Set<GraphPermission> permissions = getPermissions(ac, node);
		String[] strings = new String[permissions.size()];
		Iterator<GraphPermission> it = permissions.iterator();
		for (int i = 0; i < permissions.size(); i++) {
			strings[i] = it.next().getSimpleName();
		}
		return strings;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Set<GraphPermission> getPermissions(InternalActionContext ac, MeshVertex node) {
		String mapKey = "permissions:" + node.getUuid();
		return (Set<GraphPermission>) ac.data().computeIfAbsent(mapKey, key -> {
			Set<GraphPermission> graphPermissions = new HashSet<>();
			for (GraphPermission perm : GraphPermission.values()) {
				if (hasPermission(ac, node, perm)) {
					graphPermissions.add(perm);
				}
			}
			return graphPermissions;
		});
	}

	@Override
	public boolean hasPermission(InternalActionContext ac, MeshVertex node, GraphPermission permission) {
		if (log.isTraceEnabled()) {
			log.debug("Checking permissions for vertex {" + node.getUuid() + "}");
		}
		String mapKey = "permission:" + permission.label() + ":" + node.getUuid();
		return (boolean) ac.data().computeIfAbsent(mapKey, key -> {
			Iterable<Vertex> groups = getElement().getVertices(Direction.OUT, HAS_USER);
			for (Vertex group : groups) {
				if (log.isTraceEnabled()) {
					log.trace("Group: " + group.getProperty("name") + " - uuid: " + group.getProperty("uuid") + " - type: "
							+ group.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
				}
				Iterable<Vertex> roles = group.getVertices(Direction.IN, HAS_ROLE);
				for (Vertex role : roles) {
					if (log.isTraceEnabled()) {
						log.trace("Role: " + role.getProperty("name") + " - uuid: " + role.getProperty("uuid") + " - type: "
								+ role.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY));
					}
					// TODO maybe it would be better to use this orientdb extension. 
					//					Iterable<Edge> permissions = ((OrientVertex) role).getEdges((OrientVertex)node.getImpl().getElement(), Direction.OUT, permission.label());
					//					for (Edge permissionEdge : permissions) {
					//						return true;
					//					}
					//					return false;

					Iterable<Edge> permissions = role.getEdges(Direction.OUT, permission.label());
					for (Edge permissionEdge : permissions) {
						if (log.isTraceEnabled()) {
							log.trace("Permission Edge: " + permissionEdge.getProperty("uuid") + " - label: " + permissionEdge.getLabel()
									+ " - type: " + permissionEdge.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY) + " from: "
									+ permissionEdge.getVertex(Direction.OUT).getProperty("uuid") + " to: "
									+ permissionEdge.getVertex(Direction.IN).getProperty("uuid"));
						}

						if (node.getImpl().getId().equals(permissionEdge.getVertex(Direction.IN).getId())) {
							if (log.isTraceEnabled()) {
								log.trace("Found edge to specified node. User has permission.");
							}
							return true;
						}
					}
				}
			}
			return false;
		});
		// return out(HAS_USER).in(HAS_ROLE).outE(permission.label()).mark().inV().retain(node.getImpl()).hasNext();

	}

	@Override
	public User hasPermission(InternalActionContext ac, MeshVertex vertex, GraphPermission permission, Handler<AsyncResult<Boolean>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrx(noTrx -> {
			boolean result = hasPermission(ac, vertex, permission);
			handler.handle(Future.succeededFuture(result));
		} , rh -> {
			handler.handle(Future.succeededFuture());
		});
		return this;
	}

	@Override
	public User transformToRest(InternalActionContext ac, Handler<AsyncResult<UserResponse>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();

		db.asyncNoTrx(noTrx -> {
			Set<ObservableFuture<Void>> futures = new HashSet<>();
			UserResponse restUser = new UserResponse();
			fillRest(restUser, ac);
			restUser.setUsername(getUsername());
			restUser.setEmailAddress(getEmailAddress());
			restUser.setFirstname(getFirstname());
			restUser.setLastname(getLastname());

			Node node = getReferencedNode();
			if (node != null) {
				boolean expandReference = ac.getExpandedFieldnames().contains("nodeReference");
				ObservableFuture<Void> obsNodeReference = RxHelper.observableFuture();
				futures.add(obsNodeReference);
				if (expandReference) {
					node.transformToRest(ac, rh -> {
						if (rh.succeeded()) {
							restUser.setNodeReference(rh.result());
							obsNodeReference.toHandler().handle(Future.succeededFuture());
						} else {
							obsNodeReference.toHandler().handle(Future.failedFuture(rh.cause()));
						}
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
					obsNodeReference.toHandler().handle(Future.succeededFuture());
				}

			}
			for (Group group : getGroups()) {
				restUser.addGroup(group.getName());
			}

			// Prevent errors in which no futures have been added
			ObservableFuture<Void> obsFieldSet = RxHelper.observableFuture();
			futures.add(obsFieldSet);
			obsFieldSet.toHandler().handle(Future.succeededFuture());

			// Wait for all async processes to complete
			Observable.merge(futures).subscribe(item -> {
			} , error -> {
				noTrx.fail(error);
			} , () -> {
				noTrx.complete(restUser);
			});

		} , (AsyncResult<UserResponse> rh) -> {
			handler.handle(rh);
		});

		return this;
	}

	@Override
	public User transformToUserReference(Handler<AsyncResult<UserReference>> handler) {
		UserReference reference = new UserReference();
		reference.setName(getUsername());
		reference.setUuid(getUuid());
		handler.handle(Future.succeededFuture(reference));
		return this;
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
		disable();
		// TODO we should not really delete users. Instead we should remove those from all groups and deactivate the access.
		if (log.isDebugEnabled()) {
			log.debug("Deleting user. The user will not be deleted. Instead the user will be just disabled and removed from all groups.");
		}
		outE(HAS_USER).removeAll();
	}

	/**
	 * Encode the given password and set the generated hash.
	 * 
	 * @param password
	 */
	@Override
	public void setPassword(String password) {
		setPasswordHash(getInstance().passwordEncoder().encode(password));
	}

	@Override
	public void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();

		try {
			UserUpdateRequest requestModel = JsonUtil.readNode(ac.getBodyAsString(), UserUpdateRequest.class, ServerSchemaStorage.getSchemaStorage());
			db.trx(txUpdate -> {
				if (requestModel.getUsername() != null && !getUsername().equals(requestModel.getUsername())) {
					if (BootstrapInitializer.getBoot().userRoot().findByUsername(requestModel.getUsername()) != null) {
						handler.handle(failedFuture(ac, CONFLICT, "user_conflicting_username"));
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
					setPasswordHash(MeshSpringConfiguration.getInstance().passwordEncoder().encode(requestModel.getPassword()));
				}

				// TODO use fillRest method instead
				setEditor(ac.getUser());
				setLastEditedTimestamp(System.currentTimeMillis());
				if (requestModel.getNodeReference() != null) {
					NodeReference reference = requestModel.getNodeReference();
					// TODO also handle full node response inside node reference field
					if (reference instanceof NodeReferenceImpl) {
						NodeReferenceImpl basicReference = ((NodeReferenceImpl) reference);
						if (isEmpty(basicReference.getProjectName()) || isEmpty(reference.getUuid())) {
							txUpdate.fail(error(ac, BAD_REQUEST, "user_incomplete_node_reference"));
							return;
						} else {
							String referencedNodeUuid = basicReference.getUuid();
							String projectName = basicReference.getProjectName();
							/* TODO decide whether we need to check perms on the project as well */
							Project project = BootstrapInitializer.getBoot().projectRoot().findByName(projectName);
							if (project == null) {
								txUpdate.fail(error(ac, BAD_REQUEST, "project_not_found", projectName));
								return;
							} else {
								NodeRoot nodeRoot = project.getNodeRoot();
								Node node = loadObjectByUuidBlocking(ac, referencedNodeUuid, READ_PERM, nodeRoot);
								setReferencedNode(node);
								SearchQueueBatch batch = addIndexBatch(UPDATE_ACTION);
								txUpdate.complete(batch);
								//								return;
							}
						}
					}
				} else {
					SearchQueueBatch batch = addIndexBatch(UPDATE_ACTION);
					txUpdate.complete(batch);
				}
			} , (AsyncResult<SearchQueueBatch> userUpdated) -> {
				if (userUpdated.failed()) {
					handler.handle(Future.failedFuture(userUpdated.cause()));
				} else {
					processOrFail2(ac, userUpdated.result(), handler);
				}
			});

		} catch (IOException e) {
			handler.handle(Future.failedFuture(e));
		}

	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// for (GenericVertex<?> element : getCreatedElements()) {
		// batch.addEntry(element, UPDATE_ACTION);
		// }
		// for (GenericVertex<?> element : getEditedElements()) {
		// batch.addEntry(element, UPDATE_ACTION);
		// }
	}

}
