package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_GROUP;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_REFERENCE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getInstance;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
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
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;
import rx.subjects.AsyncSubject;

/**
 * @see User
 */
public class UserImpl extends AbstractMeshCoreVertex<UserResponse, User> implements User {

	private static final Logger log = LoggerFactory.getLogger(UserImpl.class);

	public static final String FIRSTNAME_PROPERTY_KEY = "firstname";

	public static final String LASTNAME_PROPERTY_KEY = "lastname";

	public static final String USERNAME_PROPERTY_KEY = "username";

	public static final String EMAIL_PROPERTY_KEY = "emailAddress";

	public static final String PASSWORD_HASH_PROPERTY_KEY = "passwordHash";

	public static final String ENABLED_FLAG_PROPERTY_KEY = "enabledFlag";

	public static void checkIndices(Database database) {
		database.addVertexType(UserImpl.class);
	}

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
	public void setName(String name) {
		setUsername(name);
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
		return out(HAS_USER).in(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	@Override
	public List<? extends Role> getRolesViaShortcut() {
		return out(ASSIGNED_TO_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	@Override
	public Node getReferencedNode() {
		return out(HAS_NODE_REFERENCE).has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public void setReferencedNode(Node node) {
		setUniqueLinkOutTo(node.getImpl(), HAS_NODE_REFERENCE);
	}

	@Override
	public Observable<List<String>> getPermissionNamesAsync(InternalActionContext ac, MeshVertex node) {

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
			return Observable.just(permissions);
		} else {
			List<Observable<PermResult>> futures = new ArrayList<>();

			for (GraphPermission perm : GraphPermission.values()) {
				AsyncSubject<PermResult> obs = AsyncSubject.create();
				futures.add(obs);
				// TODO Checking permissions asynchronously requires a reload of the user object and therefore the perm check is slower. We need to check
				// whether we want to still reload the user.
				// hasPermission(ac, node, perm, rh -> {
				// if (rh.failed()) {
				// obs.onError(rh.cause());
				// } else {
				// obs.onNext(new PermResult(perm, rh.result()));
				// obs.onCompleted();
				// }
				// });

				obs.onNext(new PermResult(perm, hasPermissionSync(ac, node, perm)));
				obs.onCompleted();

			}

			return Observable.merge(futures).filter(res -> res.flag).map(res -> res.perm.getSimpleName()).toList().map(list -> {
				ac.data().put(mapKey, list);
				return list;
			});

		}
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
		// String mapKey = "permissions:" + node.getUuid();
		// return (Set<GraphPermission>) ac.data().computeIfAbsent(mapKey, key -> {
		Set<GraphPermission> graphPermissions = new HashSet<>();
		for (GraphPermission perm : GraphPermission.values()) {
			if (hasPermissionSync(ac, node, perm)) {
				graphPermissions.add(perm);
			}
		}
		return graphPermissions;
		// });
	}

	@Override
	public boolean hasAdminRole() {
		for (Role role : getRolesViaShortcut()) {
			if ("admin".equals(role.getName())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean hasPermission(MeshVertex vertex, GraphPermission permission) {
		FramedGraph graph = Database.getThreadLocalGraph();
		Iterable<Edge> roleEdges = graph.getEdges("e." + ASSIGNED_TO_ROLE, this.getId());
		for (Edge roleEdge : roleEdges) {
			Vertex role = roleEdge.getVertex(Direction.IN);
			Iterable<Edge> edges = graph.getEdges("e." + permission.label(),
					MeshSpringConfiguration.getInstance().database().createComposedIndexKey(role.getId(), vertex.getImpl().getId()));
			boolean foundPermEdge = edges.iterator().hasNext();
			if (foundPermEdge) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean hasPermissionSync(InternalActionContext ac, MeshVertex node, GraphPermission permission) {
		if (log.isTraceEnabled()) {
			log.debug("Checking permissions for vertex {" + node.getUuid() + "}");
		}

		if (ac.data() == null) {
			return hasPermission(node, permission);
		}
		String mapKey = getPermissionMapKey(node, permission);
		return (boolean) ac.data().computeIfAbsent(mapKey, key -> {
			return hasPermission(node, permission);
		});
	}

	@Override
	public Observable<Boolean> hasPermissionAsync(InternalActionContext ac, MeshVertex vertex, GraphPermission permission) {
		if (ac.data() != null) {
			Boolean perm = (Boolean) ac.data().get(getPermissionMapKey(vertex, permission));
			if (perm != null) {
				return Observable.just(perm);
			}
		}
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrx(() -> hasPermission(vertex, permission));
	}

	/**
	 * Return the map key for the action context data field that may hold the fetched permission.
	 * 
	 * @param vertex
	 * @param permission
	 * @return
	 */
	private String getPermissionMapKey(MeshVertex vertex, GraphPermission permission) {
		return "perm:" + permission.label() + ":" + vertex.getUuid();
	}

	@Override
	public Observable<UserResponse> transformToRestSync(InternalActionContext ac, String... languageTags) {
		Set<Observable<UserResponse>> obs = new HashSet<>();
		UserResponse restUser = new UserResponse();

		restUser.setUsername(getUsername());
		restUser.setEmailAddress(getEmailAddress());
		restUser.setFirstname(getFirstname());
		restUser.setLastname(getLastname());
		restUser.setEnabled(isEnabled());

		// Users's node reference
		obs.add(setNodeReference(ac, restUser));

		// User's groups
		obs.add(setGroups(ac, restUser));

		// User's role permissions
		obs.add(setRolePermissions(ac, restUser));

		// User's common fields 
		obs.add(fillCommonRestFields(ac, restUser));

		// Wait for all async processes to complete
		return Observable.merge(obs).last();
		//reduce(restUser, (a, b) -> restUser);
	}

	private Observable<UserResponse> setGroups(InternalActionContext ac, UserResponse restUser) {
		for (Group group : getGroups()) {
			GroupReference reference = group.transformToReference();
			restUser.getGroups().add(reference);
		}
		return Observable.just(restUser);
	}

	/**
	 * Add the node reference field to the user response (if required to)
	 * 
	 * @param ac
	 * @param restUser
	 * @return
	 */
	private Observable<UserResponse> setNodeReference(InternalActionContext ac, UserResponse restUser) {
		Node node = getReferencedNode();
		if (node == null) {
			return Observable.empty();
		} else {
			boolean expandReference = ac.getExpandedFieldnames().contains("nodeReference") || ac.getExpandAllFlag();
			if (expandReference) {
				return node.transformToRest(ac).map(transformedNode -> {
					restUser.setNodeReference(transformedNode);
					return restUser;
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
				return Observable.just(restUser);
			}

		}

	}

	@Override
	public void addGroup(Group group) {
		// Redirect to group implementation
		group.addUser(this);
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
		// TODO don't allow this for the admin user
		// disable();
		// TODO we should not really delete users. Instead we should remove those from all groups and deactivate the access.
		// if (log.isDebugEnabled()) {
		// log.debug("Deleting user. The user will not be deleted. Instead the user will be just disabled and removed from all groups.");
		// }
		// outE(HAS_USER).removeAll();
		createIndexBatch(DELETE_ACTION);
		getElement().remove();
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
	public Observable<User> update(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		try {
			UserUpdateRequest requestModel = JsonUtil.readValue(ac.getBodyAsString(), UserUpdateRequest.class);
			return db.trx(() -> {
				if (shouldUpdate(requestModel.getUsername(), getUsername())) {
					User conflictingUser = BootstrapInitializer.getBoot().userRoot().findByUsername(requestModel.getUsername());
					if (conflictingUser != null && !conflictingUser.getUuid().equals(getUuid())) {
						throw conflict(conflictingUser.getUuid(), requestModel.getUsername(), "user_conflicting_username");
					}
					setUsername(requestModel.getUsername());
				}

				if (shouldUpdate(requestModel.getFirstname(), getFirstname())) {
					setFirstname(requestModel.getFirstname());
				}

				if (shouldUpdate(requestModel.getLastname(), getLastname())) {
					setLastname(requestModel.getLastname());
				}

				if (shouldUpdate(requestModel.getEmailAddress(), getEmailAddress())) {
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
							throw error(BAD_REQUEST, "user_incomplete_node_reference");
						}
						String referencedNodeUuid = basicReference.getUuid();
						String projectName = basicReference.getProjectName();
						/* TODO decide whether we need to check perms on the project as well */
						Project project = BootstrapInitializer.getBoot().projectRoot().findByName(projectName).toBlocking().single();
						if (project == null) {
							throw error(BAD_REQUEST, "project_not_found", projectName);
						}
						NodeRoot nodeRoot = project.getNodeRoot();
						Node node = nodeRoot.loadObjectByUuid(ac, referencedNodeUuid, READ_PERM).toBlocking().last();
						setReferencedNode(node);
					}
				}
				return createIndexBatch(UPDATE_ACTION);
			}).process().map(i -> this);

		} catch (IOException e) {
			return Observable.error(e);
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

	@Override
	public UserReference createEmptyReferenceModel() {
		return new UserReference();
	}

}
