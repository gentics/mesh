package com.gentics.mesh.context.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.context.AbstractInternalActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.netty.handler.codec.http.HttpResponseStatus;
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
public class NodeMigrationActionContextImpl extends AbstractInternalActionContext {

	private Map<String, Object> data;

	private MultiMap parameters = MultiMap.caseInsensitiveMultiMap();

	private String body;

	private String query;

	private Project project;

	private Release release;

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
		// TODO Auto-generated method stub
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
	public Project getProject() {
		return project;
	}

	public void setRelease(Release release) {
		this.release = release;
	}

	@Override
	public Release getRelease(Project project) {
		return release;
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
			public String getType() {
				return null;
			}

			@Override
			public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {

			}

			@Override
			public SearchQueueBatch addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action, boolean addRelatedEntries) {
				return null;
			}

			@Override
			public void setUuid(String uuid) {

			}

			@Override
			public void reload() {

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
			public void delete(SearchQueueBatch batch) {

			}

			@Override
			public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
					Set<GraphPermission> permissionsToRevoke) {

			}

			@Override
			public User update(InternalActionContext ac, SearchQueueBatch batch) {
				return null;
			}

			@Override
			public void setUsername(String string) {

			}

			@Override
			public void setReferencedNode(Node node) {

			}

			@Override
			public void setPasswordHash(String hash) {

			}

			@Override
			public void setPassword(String password) {

			}

			@Override
			public void setLastname(String lastname) {

			}

			@Override
			public void setFirstname(String firstname) {

			}

			@Override
			public void setEmailAddress(String email) {

			}

			@Override
			public boolean isEnabled() {
				return true;
			}

			@Override
			public void inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {
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
			public boolean hasAdminRole() {
				return false;
			}

			@Override
			public String getUsername() {
				return "node_migration";
			}

			@Override
			public List<? extends Role> getRolesViaShortcut() {
				return Collections.emptyList();
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
			public String[] getPermissionNames(MeshVertex vertex) {
				return null;
			}

			@Override
			public String getPasswordHash() {
				return null;
			}

			@Override
			public String getLastname() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<? extends Group> getGroups() {
				return Collections.emptyList();
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
			public void enable() {
			}

			@Override
			public void disable() {
			}

			@Override
			public void deactivate() {
			}

			@Override
			public boolean canReadNode(InternalActionContext ac, Node node) {
				return true;
			}

			@Override
			public void addPermissionsOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
			}

			@Override
			public void addGroup(Group group) {
			}

			@Override
			public void addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode) {
			}

			@Override
			public io.vertx.ext.auth.User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
				// TODO Auto-generated method stub
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
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T addFramedEdge(String label, VertexFrame inVertex, ClassInitializer<T> initializer) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T addFramedEdge(String label, VertexFrame inVertex, Class<T> kind) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, ClassInitializer<T> initializer) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T addFramedEdgeExplicit(String label, VertexFrame inVertex, Class<T> kind) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public TEdge addFramedEdge(String label, VertexFrame inVertex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public TEdge addFramedEdgeExplicit(String label, VertexFrame inVertex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> out(int branchFactor, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> out(String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> in(int branchFactor, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> in(String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> both(int branchFactor, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> both(String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> outE(int branchFactor, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> outE(String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> inE(int branchFactor, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> inE(String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> bothE(int branchFactor, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> bothE(String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void linkOut(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void linkIn(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void linkBoth(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void unlinkOut(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void unlinkIn(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void unlinkBoth(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setLinkOut(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setLinkIn(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setLinkBoth(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public <K> K setLinkOut(ClassInitializer<K> initializer, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkOut(Class<K> kind, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkOutExplicit(ClassInitializer<K> initializer, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkOutExplicit(Class<K> kind, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkIn(ClassInitializer<K> initializer, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkIn(Class<K> kind, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkInExplicit(ClassInitializer<K> initializer, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkInExplicit(Class<K> kind, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkBoth(ClassInitializer<K> initializer, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkBoth(Class<K> kind, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkBothExplicit(ClassInitializer<K> initializer, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <K> K setLinkBothExplicit(Class<K> kind, String... labels) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> traversal() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public com.google.gson.JsonObject toJson() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T reframe(Class<T> kind) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T reframeExplicit(Class<T> kind) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <N> N getId() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<String> getPropertyKeys() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void remove() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void setElement(Element element) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public FramedGraph getGraph() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T getProperty(String name) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <T> T getProperty(String name, Class<T> type) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setProperty(String name, Object value) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Class<?> getTypeResolution() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setTypeResolution(Class<?> type) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void removeTypeResolution() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public VertexTraversal<?, ?, ?> v() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> e() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public VertexTraversal<?, ?, ?> v(Object... ids) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public EdgeTraversal<?, ?, ?> e(Object... ids) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public VertexTraversal<?, ?, ?> getPermTraversal(GraphPermission permission) {
				// TODO Auto-generated method stub
				return null;
			}
		};
		return user;
	}

	@Override
	public Set<FileUpload> getFileUploads() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MultiMap requestHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCookie(Cookie cookie) {
		// TODO Auto-generated method stub
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
	public void send(String body, HttpResponseStatus status) {
		// TODO Auto-generated method stub
	}

	@Override
	public void send(HttpResponseStatus status) {
		// TODO Auto-generated method stub
	}

	@Override
	public void fail(Throwable cause) {
		// TODO Auto-generated method stub
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
	}

	@Override
	public void setEtag(String entityTag, boolean isWeak) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setLocation(String location) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean matches(String etag, boolean isWeak) {
		return false;
	}

}
