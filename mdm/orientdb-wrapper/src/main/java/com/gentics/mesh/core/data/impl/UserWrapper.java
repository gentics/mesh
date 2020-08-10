package com.gentics.mesh.core.data.impl;

import java.util.Set;
import java.util.function.Function;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibUser;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.google.gson.JsonObject;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import io.reactivex.Single;
import io.vertx.core.Vertx;

// TODO Change the interface to the MDM API User Interface once ready
public class UserWrapper implements User, HibUser {

	private final User delegate;

	public static UserWrapper wrap(User user) {
		if (user == null) {
			return null;
		} else {
			return new UserWrapper(user);
		}
	}

	public UserWrapper(User delegate) {
		this.delegate = delegate;
	}

	public Object id() {
		return delegate.id();
	}

	public void setCreated(HibUser creator) {
		delegate.setCreated(creator);
	}

	public UserReference transformToReference() {
		return delegate.transformToReference();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return delegate.getRolePermissions(ac, roleUuid);
	}

	public UserWrapper getEditor() {
		return UserWrapper.wrap(delegate.getEditor());
	}

	public UserWrapper getCreator() {
		return UserWrapper.wrap(delegate.getCreator());
	}

	public void setUuid(String uuid) {
		delegate.setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkOutTo(vertex, labels);
	}

	public String getName() {
		return delegate.getName();
	}

	public String getAPIPath(InternalActionContext ac) {
		return delegate.getAPIPath(ac);
	}

	@Override
	public void setEditor(HibUser user) {
		delegate.setEditor(user);
	}

	@Override
	public void setCreator(HibUser user) {
		delegate.setCreator(user);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return delegate.getRolesWithPerm(perm);
	}

	@Override
	public String getUuid() {
		return delegate.getUuid();
	}

	@Override
	public void setName(String name) {
		delegate.setName(name);
	}

	@Override
	public Vertex getElement() {
		return delegate.getElement();
	}

	public Single<UserResponse> transformToRest(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRest(ac, level, languageTags);
	}

	public Vertex getVertex() {
		return delegate.getVertex();
	}

	public String getElementVersion() {
		return delegate.getElementVersion();
	}

	public Long getLastEditedTimestamp() {
		return delegate.getLastEditedTimestamp();
	}

	public Long getCreationTimestamp() {
		return delegate.getCreationTimestamp();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		delegate.setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return delegate.property(name);
	}

	public void delete(BulkActionContext bac) {
		delegate.delete(bac);
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		delegate.setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return delegate.getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delegate.addFramedEdge(label, inVertex, initializer);
	}

	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.update(ac, batch);
	}

	public void setLastEditedTimestamp(long timestamp) {
		delegate.setLastEditedTimestamp(timestamp);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		delegate.setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return delegate.getPropertyKeys();
	}

	public String getCreationDate() {
		return delegate.getCreationDate();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		delegate.addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return delegate.out(labels);
	}

	public void remove() {
		delegate.remove();
	}

	public void delete() {
		delegate.delete();
	}

	public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRestSync(ac, level, languageTags);
	}

	public void setCreationTimestamp(long timestamp) {
		delegate.setCreationTimestamp(timestamp);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return delegate.out(label, clazz);
	}

	public void setLastEditedTimestamp() {
		delegate.setLastEditedTimestamp();
	}

	public FramedGraph getGraph() {
		return delegate.getGraph();
	}

	public <R> void property(String key, R value) {
		delegate.property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		delegate.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return delegate.outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return delegate.getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return delegate.in(label, clazz);
	}

	public String getLastEditedDate() {
		return delegate.getLastEditedDate();
	}

	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		delegate.fillCommonRestFields(ac, fields, model);
	}

	public void setCreationTimestamp() {
		delegate.setCreationTimestamp();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		delegate.removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return delegate.inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return delegate.traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return delegate.getProperty(name, type);
	}

	public Database db() {
		return delegate.db();
	}

	public String getETag(InternalActionContext ac) {
		return delegate.getETag(ac);
	}

	public Vertx vertx() {
		return delegate.vertx();
	}

	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	public MeshOptions options() {
		return delegate.options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return delegate.addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public MeshElementEventModel onCreated() {
		return delegate.onCreated();
	}

	public void setCachedUuid(String uuid) {
		delegate.setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		delegate.setProperty(name, value);
	}

	public MeshElementEventModel onUpdated() {
		return delegate.onUpdated();
	}

	public TypeInfo getTypeInfo() {
		return delegate.getTypeInfo();
	}

	public MeshElementEventModel onDeleted() {
		return delegate.onDeleted();
	}

	public Class<?> getTypeResolution() {
		return delegate.getTypeResolution();
	}

	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		return delegate.onPermissionChanged(role);
	}

	public void setTypeResolution(Class<?> type) {
		delegate.setTypeResolution(type);
	}

	public String getUsername() {
		return delegate.getUsername();
	}

	public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
		delegate.fillPermissionChanged(model, role);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return delegate.addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		delegate.removeTypeResolution();
	}

	public User setUsername(String string) {
		return delegate.setUsername(string);
	}

	public VertexTraversal<?, ?, ?> v() {
		return delegate.v();
	}

	public String getEmailAddress() {
		return delegate.getEmailAddress();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return delegate.e();
	}

	public User setEmailAddress(String email) {
		return UserWrapper.wrap(delegate.setEmailAddress(email));
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return delegate.e(ids);
	}

	public String getLastname() {
		return delegate.getLastname();
	}

	public User setLastname(String lastname) {
		return UserWrapper.wrap(delegate.setLastname(lastname));
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return delegate.addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return delegate.getGraphAttribute(key);
	}

	public String getFirstname() {
		return delegate.getFirstname();
	}

	public User setFirstname(String firstname) {
		return UserWrapper.wrap(delegate.setFirstname(firstname));
	}

	public String getPasswordHash() {
		return delegate.getPasswordHash();
	}

	public User setPasswordHash(String hash) {
		return UserWrapper.wrap(delegate.setPasswordHash(hash));
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return delegate.in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return delegate.outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return delegate.inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkOut(vertex, labels);
	}

	public NodeWrapper getReferencedNode() {
		return NodeWrapper.wrap(delegate.getReferencedNode());
	}

	public User setReferencedNode(Node node) {
		return UserWrapper.wrap(delegate.setReferencedNode(node));
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.linkIn(vertex, labels);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.unlinkIn(vertex, labels);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		delegate.setLinkOut(vertex, labels);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return delegate.traversal();
	}

	public JsonObject toJson() {
		return delegate.toJson();
	}

	public <T> T reframe(Class<T> kind) {
		return delegate.reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return delegate.reframeExplicit(kind);
	}

	public Page<? extends Group> getGroups(User user, PagingParameters params) {
		return delegate.getGroups(user, params);
	}

	public TraversalResult<? extends Group> getGroups() {
		return GroupWrapper.wrap(delegate.getGroups());
	}

	public User addGroup(Group group) {
		return UserWrapper.wrap(delegate.addGroup(group));
	}

	public String getRolesHash() {
		return delegate.getRolesHash();
	}

	public Iterable<? extends Role> getRoles() {
		return delegate.getRoles();
	}

	public Iterable<? extends Role> getRolesViaShortcut() {
		return delegate.getRolesViaShortcut();
	}

	public Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params) {
		return delegate.getRolesViaShortcut(user, params);
	}

	public void updateShortcutEdges() {
		delegate.updateShortcutEdges();
	}

	public User disable() {
		return delegate.disable();
	}

	public boolean isEnabled() {
		return delegate.isEnabled();
	}

	public User enable() {
		return delegate.enable();
	}

	public User setResetToken(String token) {
		return delegate.setResetToken(token);
	}

	public String getResetToken() {
		return delegate.getResetToken();
	}

	public boolean isForcedPasswordChange() {
		return delegate.isForcedPasswordChange();
	}

	public User setForcedPasswordChange(boolean force) {
		return delegate.setForcedPasswordChange(force);
	}

	public Long getResetTokenIssueTimestamp() {
		return delegate.getResetTokenIssueTimestamp();
	}

	public User setResetTokenIssueTimestamp(Long timestamp) {
		return delegate.setResetTokenIssueTimestamp(timestamp);
	}

	public User invalidateResetToken() {
		return delegate.invalidateResetToken();
	}

	public String getAPIKeyTokenCode() {
		return delegate.getAPIKeyTokenCode();
	}

	public User setAPITokenId(String code) {
		return delegate.setAPITokenId(code);
	}

	public Long getAPITokenIssueTimestamp() {
		return delegate.getAPITokenIssueTimestamp();
	}

	public User setAPITokenIssueTimestamp() {
		return delegate.setAPITokenIssueTimestamp();
	}

	public User setAPITokenIssueTimestamp(Long timestamp) {
		return delegate.setAPITokenIssueTimestamp(timestamp);
	}

	public String getAPITokenIssueDate() {
		return delegate.getAPITokenIssueDate();
	}

	public void resetAPIToken() {
		delegate.resetAPIToken();
	}

	public MeshAuthUser toAuthUser() {
		return delegate.toAuthUser();
	}

	public boolean isAdmin() {
		return delegate.isAdmin();
	}

	public void setAdmin(boolean flag) {
		delegate.setAdmin(flag);
	}

}
