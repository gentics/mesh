package com.gentics.mesh.context.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.node.SchemaMigrationCause;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;

/**
 * Action context implementation which will be used within the node migration.
 */
public class NodeMigrationActionContextImpl extends AbstractInternalActionContext implements NodeMigrationActionContext {

	private Map<String, Object> data;

	private Set<ConflictWarning> conflicts = new HashSet<>();

	private MultiMap parameters = MultiMap.caseInsensitiveMultiMap();

	private String body;

	private String query;

	private Project project;

	private Branch branch;

	private SchemaMigrationCause cause;

	private SchemaContainerVersion fromContainerVersion;

	private SchemaContainerVersion toContainerVersion;

	private MigrationStatusHandler status;

	@Override
	public Branch getBranch() {
		return branch;
	}

	/**
	 * Set the body.
	 *
	 * @param body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * Set the query.
	 *
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	@Override
	public Map<String, Object> data() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}

	@Override
	public String query() {
		return query;
	}

	@Override
	public String getBodyAsString() {
		return body;
	}

	@Override
	public void setUser(MeshAuthUser user) {

	}

	@Override
	public Project getProject() {
		return project;
	}

	/**
	 * Set the project
	 *
	 * @param project
	 */
	public void setProject(Project project) {
		this.project = project;
	}

	@Override
	public Branch getBranch(Project project) {
		return branch;
	}

	public void setBranch(Branch branch) {
		this.branch = branch;
	}

	@Override
	public MeshAuthUser getUser() {
		// Create mocked user which has any permission
		MeshAuthUser user = new MeshAuthUser() {

			@Override
			public void setName(String name) {
			}

			@Override
			public String getName() {
				return "node_migration";
			}

			@Override
			public UserReference transformToReference() {
				return null;
			}

			@Override
			public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
				return null;
			}

			@Override
			public String getETag(InternalActionContext ac) {
				return null;
			}

			@Override
			public String getAPIPath(InternalActionContext ac) {
				return null;
			}

			@Override
			public void setUuid(String uuid) {

			}

			@Override
			public String getUuid() {
				return null;
			}

			@Override
			public Vertex getVertex() {
				return null;
			}

			@Override
			public void delete(BulkActionContext context) {

			}

			@Override
			public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
				Set<GraphPermission> permissionsToRevoke) {

			}

			@Override
			public boolean update(InternalActionContext ac, EventQueueBatch batch) {
				return true;
			}

			@Override
			public User setUsername(String string) {
				return this;
			}

			@Override
			public User setReferencedNode(Node node) {
				return this;
			}

			@Override
			public User setPasswordHash(String hash) {
				return this;
			}

			@Override
			public User setPassword(String password) {
				return this;
			}

			@Override
			public User setLastname(String lastname) {
				return this;
			}

			@Override
			public User setFirstname(String firstname) {
				return this;
			}

			@Override
			public User setEmailAddress(String email) {
				return this;
			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public User inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {
				return this;
			}

			@Override
			public boolean hasPermissionForId(Object elementId, GraphPermission permission) {
				return true;
			}

			@Override
			public boolean hasPermission(MeshVertex element, GraphPermission permission) {
				return true;
			}

			@Override
			public void failOnNoReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {

			}

			@Override
			public boolean hasAdminRole() {
				return false;
			}

			@Override
			public String getUsername() {
				return "node_migration";
			}

			@Override
			public Iterable<? extends Role> getRolesViaShortcut() {
				return Collections.emptyList();
			}

			@Override
			public Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params) {
				return null;
			}

			@Override
			public void updateShortcutEdges() {
			}

			@Override
			public String getRolesHash() {
				return null;
			}

			@Override
			public List<? extends Role> getRoles() {
				return Collections.emptyList();
			}

			@Override
			public Node getReferencedNode() {
				return null;
			}

			@Override
			public Set<GraphPermission> getPermissions(MeshVertex vertex) {
				return new HashSet<GraphPermission>(Arrays.asList(GraphPermission.values()));
			}

			@Override
			public String getPasswordHash() {
				return null;
			}

			@Override
			public String getLastname() {
				return null;
			}

			@Override
			public Page<? extends Group> getGroups(User user, PagingParameters params) {
				return null;
			}

			@Override
			public TraversalResult<? extends Group> getGroups() {
				return new TraversalResult<>(() -> Collections.emptyIterator());
			}

			@Override
			public String getFirstname() {
				return null;
			}

			@Override
			public String getEmailAddress() {
				return null;
			}

			@Override
			public User enable() {
				return this;
			}

			@Override
			public User disable() {
				return this;
			}

			@Override
			public User deactivate() {
				return this;
			}

			@Override
			public boolean canReadNode(InternalActionContext ac, Node node) {
				return true;
			}

			@Override
			public User addPermissionsOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
				return this;
			}

			@Override
			public User addGroup(Group group) {
				return this;
			}

			@Override
			public User addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode) {
				return this;
			}

			@Override
			public io.vertx.ext.auth.User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
				return null;
			}

			@Override
			public io.vertx.ext.auth.User clearCache() {
				return null;
			}

			@Override
			public JsonObject principal() {
				return null;
			}

			@Override
			public void setAuthProvider(AuthProvider authProvider) {
			}

			@Override
			public void writeToBuffer(Buffer buffer) {
			}

			@Override
			public int readFromBuffer(int pos, Buffer buffer) {
				return 0;
			}

			@Override
			public Vertex getElement() {

				return null;
			}

			@Override
			public <T> T addFramedEdge(String label, VertexFrame inVertex, ClassInitializer<T> initializer) {

				return null;
			}

			@Override
			public <T> T addFramedEdge(String label, VertexFrame inVertex, Class<T> kind) {

				return null;
			}

			@Override
			public <T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, ClassInitializer<T> initializer) {

				return null;
			}

			@Override
			public <T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, Class<T> kind) {

				return null;
			}

			@Override
			public TEdge addFramedEdge(String label, VertexFrame inVertex) {

				return null;
			}

			@Override
			public TEdge addFramedEdgeExplicit(String label, VertexFrame inVertex) {

				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> out(int branchFactor, String... labels) {

				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> out(String... labels) {

				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> in(int branchFactor, String... labels) {

				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> in(String... labels) {

				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> both(int branchFactor, String... labels) {

				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> both(String... labels) {

				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> outE(int branchFactor, String... labels) {

				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> outE(String... labels) {

				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> inE(int branchFactor, String... labels) {

				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> inE(String... labels) {

				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> bothE(int branchFactor, String... labels) {

				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> bothE(String... labels) {

				return null;
			}

			@Override
			public void linkOut(VertexFrame vertex, String... labels) {

			}

			@Override
			public void linkIn(VertexFrame vertex, String... labels) {

			}

			@Override
			public void linkBoth(VertexFrame vertex, String... labels) {

			}

			@Override
			public void unlinkOut(VertexFrame vertex, String... labels) {

			}

			@Override
			public void unlinkIn(VertexFrame vertex, String... labels) {

			}

			@Override
			public void unlinkBoth(VertexFrame vertex, String... labels) {

			}

			@Override
			public void setLinkOut(VertexFrame vertex, String... labels) {

			}

			@Override
			public void setLinkIn(VertexFrame vertex, String... labels) {

			}

			@Override
			public void setLinkBoth(VertexFrame vertex, String... labels) {

			}

			@Override
			public <K> K setLinkOut(ClassInitializer<K> initializer, String... labels) {

				return null;
			}

			@Override
			public <K> K setLinkOut(Class<K> kind, String... labels) {

				return null;
			}

			@Override
			public <K> K setLinkOutExplicit(ClassInitializer<K> initializer, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkOutExplicit(Class<K> kind, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkIn(ClassInitializer<K> initializer, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkIn(Class<K> kind, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkInExplicit(ClassInitializer<K> initializer, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkInExplicit(Class<K> kind, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkBoth(ClassInitializer<K> initializer, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkBoth(Class<K> kind, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkBothExplicit(ClassInitializer<K> initializer, String... labels) {
				return null;
			}

			@Override
			public <K> K setLinkBothExplicit(Class<K> kind, String... labels) {
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> traversal() {
				return null;
			}

			@Override
			public com.google.gson.JsonObject toJson() {
				return null;
			}

			@Override
			public <T> T reframe(Class<T> kind) {
				return null;
			}

			@Override
			public <T> T reframeExplicit(Class<T> kind) {
				return null;
			}

			@Override
			public <N> N getId() {
				return null;
			}

			@Override
			public Set<String> getPropertyKeys() {
				return null;
			}

			@Override
			public void remove() {

			}

			@Override
			public void setElement(Element element) {

			}

			@Override
			public FramedGraph getGraph() {
				return null;
			}

			@Override
			public <T> T getProperty(String name) {
				return null;
			}

			@Override
			public <T> T getProperty(String name, Class<T> type) {
				return null;
			}

			@Override
			public void setProperty(String name, Object value) {

			}

			@Override
			public Class<?> getTypeResolution() {
				return null;
			}

			@Override
			public void setTypeResolution(Class<?> type) {

			}

			@Override
			public void removeTypeResolution() {
			}

			@Override
			public VertexTraversal<?, ?, ?> v() {
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> e() {
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> v(Object... ids) {
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> e(Object... ids) {
				return null;
			}

			@Override
			public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {

			}

			@Override
			public VertexTraversal<?, ?, ?> getPermTraversal(GraphPermission permission) {
				return null;
			}

			@Override
			public User getCreator() {
				return null;
			}

			@Override
			public User getEditor() {
				return null;
			}

			@Override
			public Single<UserResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
				return MeshInternal.get().database().asyncTx(() -> {
					return Single.just(transformToRestSync(ac, level, languageTags));
				});
			}

			@Override
			public User setResetToken(String token) {
				return this;
			}

			@Override
			public String getResetToken() {
				return null;
			}

			@Override
			public Long getResetTokenIssueTimestamp() {
				return null;
			}

			@Override
			public User setResetTokenIssueTimestamp(Long timestamp) {
				return null;
			}

			@Override
			public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
			}

			@Override
			public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
				return null;
			}

			@Override
			public void setRolePermissions(InternalActionContext ac, GenericRestResponse model) {
			}

			@Override
			public PermissionInfo getPermissionInfo(MeshVertex vertex) {
				return null;
			}

			@Override
			public String getElementVersion() {
				return null;
			}

			@Override
			public MeshElementEventModel onCreated() {
				return null;
			}

			@Override
			public MeshElementEventModel onUpdated() {
				return null;
			}

			@Override
			public MeshElementEventModel onDeleted() {
				return null;
			}

			@Override
			public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
				return null;
			}

			@Override
			public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
			}
		};
		return user;
	}

	@Override
	public Set<FileUpload> getFileUploads() {

		return null;
	}

	@Override
	public MultiMap requestHeaders() {

		return null;
	}

	@Override
	public void addCookie(Cookie cookie) {

	}

	@Override
	public String getParameter(String name) {
		return parameters.get(name);
	}

	@Override
	public MultiMap getParameters() {
		return parameters;
	}

	@Override
	public void setParameter(String name, String value) {
		parameters.add(name, value);
	}

	@Override
	public void send(String body, HttpResponseStatus status, String contentType) {

	}

	@Override
	public void send(HttpResponseStatus status) {

	}

	@Override
	public void fail(Throwable cause) {

	}

	@Override
	public Locale getLocale() {

		return null;
	}

	@Override
	public void logout() {

	}

	@Override
	public void setEtag(String entityTag, boolean isWeak) {

	}

	@Override
	public void setLocation(String location) {

	}

	@Override
	public boolean matches(String etag, boolean isWeak) {
		return false;
	}

	@Override
	public boolean isMigrationContext() {
		return true;
	}

	@Override
	public void setWebrootResponseType(String type) {
		// Not supported
	}

	/**
	 * Add the encountered conflict info to the context.
	 *
	 * @param info
	 */
	public void addConflictInfo(ConflictWarning info) {
		conflicts.add(info);
	}

	/**
	 * Get the set of encountered conflicts.
	 *
	 * @return
	 */
	public Set<ConflictWarning> getConflicts() {
		return conflicts;
	}

	@Override
	public SchemaMigrationCause getCause() {
		return cause;
	}

	public void setCause(SchemaMigrationCause cause) {
		this.cause = cause;
	}

	@Override
	public SchemaContainerVersion getFromVersion() {
		return fromContainerVersion;
	}

	public void setFromVersion(SchemaContainerVersion fromContainerVersion) {
		this.fromContainerVersion = fromContainerVersion;
	}

	@Override
	public SchemaContainerVersion getToVersion() {
		return toContainerVersion;
	}

	public void setToVersion(SchemaContainerVersion toContainerVersion) {
		this.toContainerVersion = toContainerVersion;
	}

	public void setStatus(MigrationStatusHandler status) {
		this.status = status;
	}

	@Override
	public MigrationStatusHandler getStatus() {
		return status;
	}

	@Override
	public void validate() {
		Objects.requireNonNull(fromContainerVersion, "The source schema reference is missing in the context.");
		Objects.requireNonNull(toContainerVersion, "The target schema reference is missing in the context.");
	}

	@Override
	public boolean isPurgeAllowed() {
		// The purge operation is now allowed during schema migrations. Instead the purge will be executed after containers have been migrated.
		return false;
	}

}
