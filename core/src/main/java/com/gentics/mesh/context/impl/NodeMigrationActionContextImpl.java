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
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.link.WebRootLinkReplacer.Type;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.query.impl.PagingParameter;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.FileUpload;
import rx.Observable;

/**
 * Action context implementation which will be used within the node migration.
 */
public class NodeMigrationActionContextImpl extends AbstractInternalActionContext {

	private Map<String, Object> data;

	private String body;

	private String query;

	private List<String> languageTags;

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

	/**
	 * Set the language tags.
	 *
	 * @param languageTags
	 */
	public void setLanguageTags(List<String> languageTags) {
		this.languageTags = languageTags;
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
	public List<String> getSelectedLanguageTags() {
		return languageTags;
	}

	@Override
	public PagingParameter getPagingParameter() {
		return PagingParameter.fromQuery(query());
	}

	@Override
	public boolean getExpandAllFlag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Type getResolveLinksType() {
		return Type.OFF;
	}

	@Override
	public void setUser(MeshAuthUser user) {
		// TODO Auto-generated method stub

	}

	@Override
	public Project getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshAuthUser getUser() {
		return new MeshAuthUser() {

			@Override
			public void writeToBuffer(Buffer buffer) {
			}

			@Override
			public int readFromBuffer(int pos, Buffer buffer) {
				return 0;
			}

			@Override
			public void setName(String name) {

			}

			@Override
			public String getName() {
				return getUsername();
			}

			@Override
			public UserReference createEmptyReferenceModel() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Observable<UserResponse> transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getType() {
				return com.gentics.mesh.core.data.User.TYPE;
			}

			@Override
			public SearchQueueBatch createIndexBatch(SearchQueueEntryAction action) {
				return null;
			}

			@Override
			public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {

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
			public Element getElement() {
				return null;
			}

			@Override
			public Vertex getVertex() {
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
			public Observable<? extends com.gentics.mesh.core.data.User> update(InternalActionContext ac) {
				return null;
			}

			@Override
			public void setLastEditedTimestamp(long timestamp) {

			}

			@Override
			public void setEditor(com.gentics.mesh.core.data.User user) {

			}

			@Override
			public void setCreator(com.gentics.mesh.core.data.User user) {

			}

			@Override
			public void setCreationTimestamp(long timestamp) {
			}

			@Override
			public void setCreated(com.gentics.mesh.core.data.User user) {
			}

			@Override
			public Long getLastEditedTimestamp() {
				return System.currentTimeMillis();
			}

			@Override
			public com.gentics.mesh.core.data.User getEditor() {
				return this;
			}

			@Override
			public com.gentics.mesh.core.data.User getCreator() {
				return this;
			}

			@Override
			public Long getCreationTimestamp() {
				return System.currentTimeMillis();
			}

			@Override
			public void setUsername(String string) {

			}

			@Override
			public void setReferencedNode(Node node) {
			}

			@Override
			public void setPasswordHash(String hash) {
				// TODO Auto-generated method stub

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
			public boolean hasPermissionSync(InternalActionContext ac, MeshVertex vertex, GraphPermission permission) {
				return true;
			}

			@Override
			public Observable<Boolean> hasPermissionAsync(InternalActionContext ac, MeshVertex vertex, GraphPermission permission) {
				return Observable.just(true);
			}

			@Override
			public boolean hasPermission(MeshVertex node, GraphPermission permission) {
				return true;
			}

			@Override
			public boolean hasAdminRole() {
				return true;
			}

			@Override
			public String getUsername() {
				return "NodeMigrationUser";
			}

			@Override
			public List<? extends Role> getRolesViaShortcut() {
				// TODO Auto-generated method stub
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
			public Set<GraphPermission> getPermissions(InternalActionContext ac, MeshVertex vertex) {
				return new HashSet<>(Arrays.asList(GraphPermission.values()));
			}

			@Override
			public Observable<List<String>> getPermissionNamesAsync(InternalActionContext ac, MeshVertex node) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String[] getPermissionNames(InternalActionContext ac, MeshVertex vertex) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getPasswordHash() {
				return null;
			}

			@Override
			public String getLastname() {
				return "Node Migration";
			}

			@Override
			public List<? extends Group> getGroups() {
				return Collections.emptyList();
			}

			@Override
			public String getFirstname() {
				return "Node Migration";
			}

			@Override
			public String getEmailAddress() {
				// TODO Auto-generated method stub
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
			public void addGroup(Group group) {
			}

			@Override
			public void addCRUDPermissionOnRole(MeshVertex sourceNode, GraphPermission permission, MeshVertex targetNode) {

			}

			@Override
			public void setAuthProvider(AuthProvider authProvider) {
				// TODO Auto-generated method stub

			}

			@Override
			public JsonObject principal() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public User clearCache() {
				return null;
			}

			@Override
			public MeshAuthUserImpl getImpl() {
				return null;
			}
		};
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void send(String body, HttpResponseStatus statusCode) {
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

}
