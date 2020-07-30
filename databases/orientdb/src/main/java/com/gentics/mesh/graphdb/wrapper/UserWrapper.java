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
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.tinkerpop.blueprints.Vertex;

public class UserWrapper extends MeshElementWrapper<User> implements AUser {

	private UserWrapper(User delegate) {
		super(delegate);
	}

	public static UserWrapper of(User delegate) {
		if (delegate == null) {
			return null;
		}
		return new UserWrapper(delegate);
	}

	@Override
	public User getDelegate() {
		return delegate;
	}

	@Override
	public TypeInfo getTypeInfo() {
		return delegate.getTypeInfo();
	}

	public static String composeIndexName() {
		return User.composeIndexName();
	}

	public static String composeDocumentId(String elementUuid) {
		return User.composeDocumentId(elementUuid);
	}

	@Override
	public String getUsername() {
		return delegate.getUsername();
	}

	@Override
	public AUser setUsername(String string) {
		delegate.setUsername(string);
		return this;
	}

	@Override
	public String getEmailAddress() {
		return delegate.getEmailAddress();
	}

	@Override
	public AUser setEmailAddress(String email) {
		delegate.setEmailAddress(email);
		return this;
	}

	@Override
	public String getLastname() {
		return delegate.getLastname();
	}

	@Override
	public AUser setLastname(String lastname) {
		delegate.setLastname(lastname);
		return this;
	}

	@Override
	public String getFirstname() {
		return delegate.getFirstname();
	}

	@Override
	public AUser setFirstname(String firstname) {
		delegate.setFirstname(firstname);
		return this;
	}

	@Override
	public String getPasswordHash() {
		return delegate.getPasswordHash();
	}

	@Override
	public AUser setPasswordHash(String hash) {
		delegate.setPasswordHash(hash);
		return this;
	}

	@Override
	public AUser setPassword(String password) {
		delegate.setPassword(password);
		return this;
	}

	@Override
	public Node getReferencedNode() {
		return delegate.getReferencedNode();
	}

	@Override
	public AUser setReferencedNode(Node node) {
		delegate.setReferencedNode(node);
		return this;
	}

	@Override
	public PermissionInfo getPermissionInfo(MeshVertex vertex) {
		return delegate.getPermissionInfo(vertex);
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex vertex) {
		return delegate.getPermissions(vertex);
	}

	@Override
	public AUser addCRUDPermissionOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode) {
		delegate.addCRUDPermissionOnRole(sourceNode, permission, targetNode);
		return this;
	}

	@Override
	public AUser addPermissionsOnRole(HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode, GraphPermission... toGrant) {
		delegate.addPermissionsOnRole(sourceNode, permission, targetNode, toGrant);
		return this;
	}

	@Override
	public AUser inheritRolePermissions(MeshVertex sourceNode, MeshVertex targetNode) {
		delegate.inheritRolePermissions(sourceNode, targetNode);
		return this;
	}

	@Override
	public boolean updateDry(InternalActionContext ac) {
		return delegate.updateDry(ac);
	}

	@Override
	public Page<? extends Group> getGroups(User user, PagingParameters params) {
		return this.delegate.getGroups(user, params);
	}

	@Override
	public TraversalResult<? extends Group> getGroups() {
		return delegate.getGroups();
	}

	@Override
	public AUser addGroup(Group group) {
		delegate.addGroup(group);
		return this;
	}

	@Override
	public String getRolesHash() {
		return delegate.getRolesHash();
	}

	@Override
	public Iterable<? extends Role> getRoles() {
		return delegate.getRoles();
	}

	@Override
	public Iterable<? extends Role> getRolesViaShortcut() {
		return delegate.getRolesViaShortcut();
	}

	@Override
	public Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params) {
		return this.delegate.getRolesViaShortcut(user, params);
	}

	@Override
	public void updateShortcutEdges() {
		delegate.updateShortcutEdges();
	}

	@Override
	public AUser disable() {
		delegate.disable();
		return this;
	}

	@Override
	public boolean isEnabled() {
		return delegate.isEnabled();
	}

	@Override
	public AUser enable() {
		delegate.enable();
		return this;
	}

	@Override
	public AUser deactivate() {
		delegate.deactivate();
		return this;
	}

	@Override
	public boolean hasPermission(MeshVertex element, GraphPermission permission) {
		return delegate.hasPermission(element, permission);
	}

	@Override
	public boolean hasPermissionForId(Object elementId, GraphPermission permission) {
		return delegate.hasPermissionForId(elementId, permission);
	}

	@Override
	public boolean hasReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		return delegate.hasReadPermission(container, branchUuid, requestedVersion);
	}

	@Override
	public void failOnNoReadPermission(NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		delegate.failOnNoReadPermission(container, branchUuid, requestedVersion);
	}

	@Override
	public boolean canReadNode(InternalActionContext ac, Node node) {
		return delegate.canReadNode(ac, node);
	}

	@Override
	public AUser setResetToken(String token) {
		delegate.setResetToken(token);
		return this;
	}

	@Override
	public String getResetToken() {
		return delegate.getResetToken();
	}

	@Override
	public boolean isForcedPasswordChange() {
		return delegate.isForcedPasswordChange();
	}

	@Override
	public AUser setForcedPasswordChange(boolean force) {
		delegate.setForcedPasswordChange(force);
		return this;
	}

	@Override
	public Long getResetTokenIssueTimestamp() {
		return delegate.getResetTokenIssueTimestamp();
	}

	@Override
	public AUser setResetTokenIssueTimestamp(Long timestamp) {
		delegate.setResetTokenIssueTimestamp(timestamp);
		return this;
	}

	@Override
	public AUser invalidateResetToken() {
		delegate.invalidateResetToken();
		return this;
	}

	@Override
	public boolean isResetTokenValid(String token, int maxTokenAgeMins) {
		return delegate.isResetTokenValid(token, maxTokenAgeMins);
	}

	@Override
	public String getAPIKeyTokenCode() {
		return delegate.getAPIKeyTokenCode();
	}

	@Override
	public AUser setAPITokenId(String code) {
		delegate.setAPITokenId(code);
		return this;
	}

	@Override
	public Long getAPITokenIssueTimestamp() {
		return delegate.getAPITokenIssueTimestamp();
	}

	@Override
	public AUser setAPITokenIssueTimestamp() {
		delegate.setAPITokenIssueTimestamp();
		return this;
	}

	@Override
	public AUser setAPITokenIssueTimestamp(Long timestamp) {
		delegate.setAPITokenIssueTimestamp(timestamp);
		return this;
	}

	@Override
	public String getAPITokenIssueDate() {
		return delegate.getAPITokenIssueDate();
	}

	@Override
	public void resetAPIToken() {
		delegate.resetAPIToken();
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return delegate.toAuthUser();
	}

	@Override
	public boolean isAdmin() {
		return delegate.isAdmin();
	}

	@Override
	public void setAdmin(boolean flag) {
		delegate.setAdmin(flag);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		return delegate.update(ac, batch);
	}

	@Override
	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		delegate.fillCommonRestFields(ac, fields, model);
	}

	@Override
	public MeshElementEventModel onCreated() {
		return delegate.onCreated();
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return delegate.onUpdated();
	}

	@Override
	public MeshElementEventModel onDeleted() {
		return delegate.onDeleted();
	}

	@Override
	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		return delegate.onPermissionChanged(role);
	}

	@Override
	public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
		delegate.fillPermissionChanged(model, role);
	}

	@Override
	public Vertex getVertex() {
		return delegate.getVertex();
	}

	@Override
	public void delete(BulkActionContext bac) {
		delegate.delete(bac);
	}

	@Override
	public void delete() {
		delegate.delete();
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		delegate.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	@Override
	public void setCachedUuid(String uuid) {
		delegate.setCachedUuid(uuid);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return delegate.getAPIPath(ac);
	}

	@Override
	public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		return delegate.transformToRestSync(ac, level, languageTags);
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return delegate.getAPIPath(ac);
	}

	@Override
	public AUser getCreator() {
		return new UserWrapper(delegate.getCreator());
	}

	@Override
	public Long getCreationTimestamp() {
		return delegate.getCreationTimestamp();
	}

	@Override
	public String getCreationDate() {
		return delegate.getCreationDate();
	}

	@Override
	public void setCreationTimestamp(long timestamp) {
		delegate.setCreationTimestamp(timestamp);
	}

	@Override
	public void setCreationTimestamp() {
		delegate.setCreationTimestamp();
	}

	@Override
	public User getEditor() {
		return delegate.getEditor();
	}

	@Override
	public Long getLastEditedTimestamp() {
		return delegate.getLastEditedTimestamp();
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {
		delegate.setLastEditedTimestamp(timestamp);
	}

	@Override
	public void setLastEditedTimestamp() {
		delegate.setLastEditedTimestamp();
	}

	@Override
	public String getLastEditedDate() {
		return delegate.getLastEditedDate();
	}

	@Override
	public void setCreator(AUser user) {
		this.delegate.setCreator(AUser.getDelegate(user));
	}

	@Override
	public void setEditor(AUser user) {
		this.delegate.setEditor(AUser.getDelegate(user));
	}


}
