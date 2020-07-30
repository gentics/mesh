package com.gentics.mesh.graphdb.wrapper;

import java.util.Set;

import com.gentics.mda.entity.AUser;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HasPermissions;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;

public class UserWrapper implements AUser {
	private final User user;

	public UserWrapper(User user) {
		this.user = user;
	}

	@Override
	public User getDelegate() {
		return user;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return user.getTypeInfo();
	}

	public static String composeIndexName() {
		return User.composeIndexName();
	}

	public static String composeDocumentId(String elementUuid) {
		return User.composeDocumentId(elementUuid);
	}

	@Override
	public String getUsername() {
		return user.getUsername();
	}

	@Override
	public AUser setUsername(String string) {
		user.setUsername(string);
		return this;
	}

	@Override
	public String getEmailAddress() {
		return user.getEmailAddress();
	}

	@Override
	public AUser setEmailAddress(String email) {
		user.setEmailAddress(email);
		return this;
	}

	@Override
	public String getLastname() {
		return user.getLastname();
	}

	@Override
	public AUser setLastname(String lastname) {
		user.setLastname(lastname);
		return this;
	}

	@Override
	public String getFirstname() {
		return user.getFirstname();
	}

	@Override
	public AUser setFirstname(String firstname) {
		user.setFirstname(firstname);
		return this;
	}

	@Override
	public String getPasswordHash() {
		return user.getPasswordHash();
	}

	@Override
	public AUser setPasswordHash(String hash) {
		user.setPasswordHash(hash);
		return this;
	}

	@Override
	public AUser setPassword(String password) {
		user.setPassword(password);
		return this;
	}

	@Override
	public Node getReferencedNode() {
		return user.getReferencedNode();
	}

	@Override
	public AUser setReferencedNode(Node node) {
		user.setReferencedNode(node);
		return this;
	}

	@Override
	public PermissionInfo getPermissionInfo(MeshVertex vertex) {
		return user.getPermissionInfo(vertex);
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex vertex) {
		return user.getPermissions(vertex);
	}

	@Override
	public AUser addCRUDPermissionOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode) {
		user.addCRUDPermissionOnRole(sourceNode, permission, targetNode);
		return this;
	}

	@Override
	public AUser addPermissionsOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
		user.addPermissionsOnRole(sourceNode, permission, targetNode, toGrant);
		return this;
	}

	@Override
	public AUser inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {
		user.inheritRolePermissions(sourceNode, targetNode);
		return this;
	}

	@Override
	public boolean updateDry(InternalActionContext ac) {
		return user.updateDry(ac);
	}

	@Override
	public Page<? extends Group> getGroups(User user, PagingParameters params) {
		return this.user.getGroups(user, params);
	}

	@Override
	public TraversalResult<? extends Group> getGroups() {
		return user.getGroups();
	}

	@Override
	public AUser addGroup(Group group) {
		user.addGroup(group);
		return this;
	}

	@Override
	public String getRolesHash() {
		return user.getRolesHash();
	}

	@Override
	public Iterable<? extends Role> getRoles() {
		return user.getRoles();
	}

	@Override
	public Iterable<? extends Role> getRolesViaShortcut() {
		return user.getRolesViaShortcut();
	}

	@Override
	public Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params) {
		return this.user.getRolesViaShortcut(user, params);
	}

	@Override
	public void updateShortcutEdges() {
		user.updateShortcutEdges();
	}

	@Override
	public AUser disable() {
		user.disable();
		return this;
	}

	@Override
	public boolean isEnabled() {
		return user.isEnabled();
	}

	@Override
	public AUser enable() {
		user.enable();
		return this;
	}

	@Override
	public AUser deactivate() {
		user.deactivate();
		return this;
	}

	@Override
	public boolean hasPermission(MeshVertex element, GraphPermission permission) {
		return user.hasPermission(element, permission);
	}

	@Override
	public boolean hasPermissionForId(Object elementId, GraphPermission permission) {
		return user.hasPermissionForId(elementId, permission);
	}

	@Override
	public boolean hasReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		return user.hasReadPermission(container, branchUuid, requestedVersion);
	}

	@Override
	public void failOnNoReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		user.failOnNoReadPermission(container, branchUuid, requestedVersion);
	}

	@Override
	public boolean canReadNode(InternalActionContext ac, Node node) {
		return user.canReadNode(ac, node);
	}

	@Override
	public AUser setResetToken(String token) {
		user.setResetToken(token);
		return this;
	}

	@Override
	public String getResetToken() {
		return user.getResetToken();
	}

	@Override
	public boolean isForcedPasswordChange() {
		return user.isForcedPasswordChange();
	}

	@Override
	public AUser setForcedPasswordChange(boolean force) {
		user.setForcedPasswordChange(force);
		return this;
	}

	@Override
	public Long getResetTokenIssueTimestamp() {
		return user.getResetTokenIssueTimestamp();
	}

	@Override
	public AUser setResetTokenIssueTimestamp(Long timestamp) {
		user.setResetTokenIssueTimestamp(timestamp);
		return this;
	}

	@Override
	public AUser invalidateResetToken() {
		user.invalidateResetToken();
		return this;
	}

	@Override
	public boolean isResetTokenValid(String token, int maxTokenAgeMins) {
		return user.isResetTokenValid(token, maxTokenAgeMins);
	}

	@Override
	public String getAPIKeyTokenCode() {
		return user.getAPIKeyTokenCode();
	}

	@Override
	public AUser setAPITokenId(String code) {
		user.setAPITokenId(code);
		return this;
	}

	@Override
	public Long getAPITokenIssueTimestamp() {
		return user.getAPITokenIssueTimestamp();
	}

	@Override
	public AUser setAPITokenIssueTimestamp() {
		user.setAPITokenIssueTimestamp();
		return this;
	}

	@Override
	public AUser setAPITokenIssueTimestamp(Long timestamp) {
		user.setAPITokenIssueTimestamp(timestamp);
		return this;
	}

	@Override
	public String getAPITokenIssueDate() {
		return user.getAPITokenIssueDate();
	}

	@Override
	public void resetAPIToken() {
		user.resetAPIToken();
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return user.toAuthUser();
	}

	@Override
	public boolean isAdmin() {
		return user.isAdmin();
	}

	@Override
	public void setAdmin(boolean flag) {
		user.setAdmin(flag);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return user.update(ac, batch);
	}

	@Override
	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		user.fillCommonRestFields(ac, fields, model);
	}

	@Override
	public MeshElementEventModel onCreated() {
		return user.onCreated();
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return user.onUpdated();
	}

	@Override
	public MeshElementEventModel onDeleted() {
		return user.onDeleted();
	}

	@Override
	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		return user.onPermissionChanged(role);
	}

	@Override
	public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
		user.fillPermissionChanged(model, role);
	}

	@Override
	public Vertex getVertex() {
		return user.getVertex();
	}

	@Override
	public void delete(BulkActionContext bac) {
		user.delete(bac);
	}

	@Override
	public void delete() {
		user.delete();
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		user.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public boolean hasPublishPermissions() {
		return user.hasPublishPermissions();
	}

	@Override
	public void setCachedUuid(String uuid) {
		user.setCachedUuid(uuid);
	}

	@Override
	public void setUuid(String uuid) {
		user.setUuid(uuid);
	}

	@Override
	public String getUuid() {
		return user.getUuid();
	}

	public Element getElement() {
		return user.getElement();
	}

	@Override
	public String getElementVersion() {
		return user.getElementVersion();
	}

	public <T> T property(String name) {
		return user.property(name);
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		user.addToStringSetProperty(propertyKey, value);
	}

	public <R> void property(String key, R value) {
		user.property(key, value);
	}

	public void removeProperty(String key) {
		user.removeProperty(key);
	}

	@Override
	public Database db() {
		return user.db();
	}

	@Override
	public Vertx vertx() {
		return user.vertx();
	}

	@Override
	public MeshOptions options() {
		return user.options();
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return user.getAPIPath(ac);
	}

	@Override
	public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return user.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return user.getAPIPath(ac);
	}

	@Override
	public AUser getCreator() {
		return new UserWrapper(user.getCreator());
	}

	@Override
	public Long getCreationTimestamp() {
		return user.getCreationTimestamp();
	}

	@Override
	public String getCreationDate() {
		return user.getCreationDate();
	}

	@Override
	public void setCreationTimestamp(long timestamp) {
		user.setCreationTimestamp(timestamp);
	}

	@Override
	public void setCreationTimestamp() {
		user.setCreationTimestamp();
	}

	@Override
	public User getEditor() {
		return user.getEditor();
	}

	@Override
	public Long getLastEditedTimestamp() {
		return user.getLastEditedTimestamp();
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {
		user.setLastEditedTimestamp(timestamp);
	}

	@Override
	public void setLastEditedTimestamp() {
		user.setLastEditedTimestamp();
	}

	@Override
	public String getLastEditedDate() {
		return user.getLastEditedDate();
	}

	@Override
	public void setCreator(AUser user) {
		this.user.setCreator(user.getDelegate());
	}

	@Override
	public void setEditor(AUser user) {
		this.user.setEditor(user.getDelegate());
	}
}
