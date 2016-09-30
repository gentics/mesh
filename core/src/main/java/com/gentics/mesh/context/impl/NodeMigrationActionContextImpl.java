package com.gentics.mesh.context.impl;

import java.util.HashMap;
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
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
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
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getETag(InternalActionContext ac) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getAPIPath(InternalActionContext ac) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getType() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
				// TODO Auto-generated method stub

			}

			@Override
			public SearchQueueBatch addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setUuid(String uuid) {
				// TODO Auto-generated method stub

			}

			@Override
			public void reload() {
				// TODO Auto-generated method stub

			}

			@Override
			public String getUuid() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Element getElement() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Vertex getVertex() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void delete(SearchQueueBatch batch) {
				// TODO Auto-generated method stub

			}

			@Override
			public void applyPermissions(Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
					Set<GraphPermission> permissionsToRevoke) {
				// TODO Auto-generated method stub

			}

			@Override
			public User update(InternalActionContext ac, SearchQueueBatch batch) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setUsername(String string) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setReferencedNode(Node node) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setPasswordHash(String hash) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setPassword(String password) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setLastname(String lastname) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setFirstname(String firstname) {
				// TODO Auto-generated method stub

			}

			@Override
			public void setEmailAddress(String email) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean isEnabled() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public void inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean hasPermissionForId(Object elementId, GraphPermission permission) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean hasPermission(MeshVertex element, GraphPermission permission) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean hasAdminRole() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public String getUsername() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<? extends Role> getRolesViaShortcut() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<? extends Role> getRoles() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Node getReferencedNode() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<GraphPermission> getPermissions(MeshVertex vertex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getPermissionNames(MeshVertex vertex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getPasswordHash() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getLastname() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public List<? extends Group> getGroups() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getFirstname() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getEmailAddress() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void enable() {
				// TODO Auto-generated method stub

			}

			@Override
			public void disable() {
				// TODO Auto-generated method stub

			}

			@Override
			public void deactivate() {
				// TODO Auto-generated method stub

			}

			@Override
			public boolean canReadNode(InternalActionContext ac, Node node) {
				return true;
			}

			@Override
			public void addPermissionsOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
				// TODO Auto-generated method stub

			}

			@Override
			public void addGroup(Group group) {
				// TODO Auto-generated method stub

			}

			@Override
			public void addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode) {
				// TODO Auto-generated method stub

			}

			@Override
			public io.vertx.ext.auth.User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public io.vertx.ext.auth.User clearCache() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public JsonObject principal() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void setAuthProvider(AuthProvider authProvider) {
				// TODO Auto-generated method stub

			}

			@Override
			public void writeToBuffer(Buffer buffer) {
				// TODO Auto-generated method stub

			}

			@Override
			public int readFromBuffer(int pos, Buffer buffer) {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public MeshAuthUserImpl getImpl() {
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
