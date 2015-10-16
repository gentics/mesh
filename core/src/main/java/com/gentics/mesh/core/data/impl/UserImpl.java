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
import java.util.ArrayList;
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
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;
import rx.subjects.AsyncSubject;

/**
 * <pre>
* {@code
* 	(u:UserImpl)-[r1:HAS_USER]->(ur:UserRootImpl)
* 	(u)-[r2:HAS_USER]->(g:GroupImpl)
 	(g)<-[r3:HAS_ROLE]-(r:RoleImpl)
* }
 * </pre>
 *
 * <p>
 * <img src="http://getmesh.io/docs/javadoc/cypher/com.gentics.mesh.core.data.impl.UserImpl.jpg" alt="">
 * </p>
 */
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
	public User getPermissionNames(InternalActionContext ac, MeshVertex node, Handler<AsyncResult<List<String>>> handler) {

		class PermResult {

			GraphPermission perm;
			Boolean flag;

			public PermResult(GraphPermission perm, Boolean flag) {
				this.perm = perm;
				this.flag = flag;
			}
		}
		String mapKey = "permissions:" + node.getUuid();
		List<String> permissions = (List<String>) ac.data().get(mapKey);
		if (permissions != null) {
			handler.handle(Future.succeededFuture(permissions));
			return this;
		} else {
			List<Observable<PermResult>> futures = new ArrayList<>();

			for (GraphPermission perm : GraphPermission.values()) {
				AsyncSubject<PermResult> obs = AsyncSubject.create();
				futures.add(obs);
				// TODO Checking permissions asynchronously requires a reload of the user object and therefore the perm check is slower. We need to check whether we want to still reload the user.  
				//				hasPermission(ac, node, perm, rh -> {
				//					if (rh.failed()) {
				//						obs.onError(rh.cause());
				//					} else {
				//						obs.onNext(new PermResult(perm, rh.result()));
				//						obs.onCompleted();
				//					}
				//				});

				obs.onNext(new PermResult(perm, hasPermission(ac, node, perm)));
				obs.onCompleted();

			}

			Observable.merge(futures).filter(res -> res.flag).map(res -> res.perm.getSimpleName()).toList().subscribe(list -> {
				ac.data().put(mapKey, list);
				handler.handle(Future.succeededFuture(list));
			} , error -> {
				handler.handle(Future.failedFuture(error));
			});
		}
		return this;
	}

	@Override
	@Deprecated
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
		//		String mapKey = "permissions:" + node.getUuid();
		//		return (Set<GraphPermission>) ac.data().computeIfAbsent(mapKey, key -> {
		Set<GraphPermission> graphPermissions = new HashSet<>();
		for (GraphPermission perm : GraphPermission.values()) {
			if (hasPermission(ac, node, perm)) {
				graphPermissions.add(perm);
			}
		}
		return graphPermissions;
		//		});
	}

	@Override
	@Deprecated
	public boolean hasPermission(InternalActionContext ac, MeshVertex node, GraphPermission permission) {
		if (log.isTraceEnabled()) {
			log.debug("Checking permissions for vertex {" + node.getUuid() + "}");
		}

		boolean useIndex = true;

		String mapKey = getPermissionMapKey(node, permission);
		return (boolean) ac.data().computeIfAbsent(mapKey, key -> {
			FramedGraph graph = Database.getThreadLocalGraph();

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

					if (useIndex) {
						Iterable<Edge> edges = graph.getEdges("e." + permission.label().toLowerCase(),
								new OCompositeKey(role.getId(), node.getImpl().getId()));
						return edges.iterator().hasNext();

					} else {
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
			}
			return false;
		});

	}

	@Override
	public User hasPermission(InternalActionContext ac, MeshVertex vertex, GraphPermission permission, Handler<AsyncResult<Boolean>> handler) {

		Boolean perm = (Boolean) ac.data().get(getPermissionMapKey(vertex, permission));
		if (perm != null) {
			handler.handle(Future.succeededFuture(perm));
			return this;
		}

		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrx(noTrx -> {
			boolean result = hasPermission(ac, vertex, permission);
			handler.handle(Future.succeededFuture(result));
		} , rh -> {
			if (rh.failed()) {
				handler.handle(Future.failedFuture(rh.cause()));
			} else {
				handler.handle(Future.succeededFuture());
			}
		});
		return this;
	}

	/**
	 * Return the map key for the action context data field that may hold the fetched permission.
	 * 
	 * @param vertex
	 * @param permission
	 * @return
	 */
	private String getPermissionMapKey(MeshVertex vertex, GraphPermission permission) {
		String mapKey = "permission:" + permission.label() + ":" + vertex.getUuid();
		return mapKey;
	}

	@Override
	public User transformToRest(InternalActionContext ac, Handler<AsyncResult<UserResponse>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();

		db.asyncNoTrx(noTrx -> {
			Set<ObservableFuture<Void>> futures = new HashSet<>();
			UserResponse restUser = new UserResponse();

			restUser.setUsername(getUsername());
			restUser.setEmailAddress(getEmailAddress());
			restUser.setFirstname(getFirstname());
			restUser.setLastname(getLastname());
			restUser.setEnabled(isEnabled());

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

			// Add common fields
			ObservableFuture<Void> obsFieldSet = RxHelper.observableFuture();
			futures.add(obsFieldSet);
			fillRest(restUser, ac, rh -> {
				if (rh.failed()) {
					obsFieldSet.toHandler().handle(Future.failedFuture(rh.cause()));
				} else {
					obsFieldSet.toHandler().handle(Future.succeededFuture());
				}
			});

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
