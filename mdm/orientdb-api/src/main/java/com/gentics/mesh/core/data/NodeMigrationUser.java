package com.gentics.mesh.core.data;

import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.user.UserReference;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;

/**
 * Create mocked user for node migrations.
 */
public class NodeMigrationUser implements MeshAuthUser {

	@Override
	public UserReference transformToReference() {
		return null;
	}

	@Override
	public String getUuid() {
		return null;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getUsername() {
		return "node_migration";
	}

	@Override
	public void updateShortcutEdges() {
	}

	@Override
	public String getRolesHash() {
		return null;
	}

	@Override
	public HibNode getReferencedNode() {
		return null;
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
	public boolean isAdmin() {
		return false;
	}

	@Override
	public void setAdmin(boolean flag) {

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
	public void remove() {
		// TODO Auto-generated method stub
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
	public String getResetToken() {
		return null;
	}

	@Override
	public boolean isForcedPasswordChange() {
		return false;
	}

	@Override
	public User setForcedPasswordChange(boolean force) {
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
	public String getAPIKeyTokenCode() {
		return null;
	}

	@Override
	public User setAPITokenId(String code) {
		return null;
	}

	@Override
	public User setAPITokenIssueTimestamp() {
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
	public HibUser setUsername(String string) {
		return this;
	}

	@Override
	public HibUser setLastname(String lastname) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public HibUser setFirstname(String firstname) {
		return this;
	}

	@Override
	public HibUser setEmailAddress(String email) {
		return this;
	}

	@Override
	public HibUser disable() {
		return this;
	}

	@Override
	public HibUser enable() {
		return this;
	}

	@Override
	public HibUser invalidateResetToken() {
		return this;
	}

	@Override
	public HibUser setPasswordHash(String hash) {
		return this;
	}

	@Override
	public Long getAPITokenIssueTimestamp() {
		return null;
	}

	@Override
	public void resetAPIToken() {

	}

	@Override
	public HibUser setResetToken(String token) {
		return this;
	}

	@Override
	public String getAPITokenIssueDate() {
		return null;
	}

	@Override
	public HibUser setReferencedNode(HibInNode node) {
		return this;
	}

	@Override
	public void setEditor(HibUser user) {

	}

	@Override
	public Long getLastEditedTimestamp() {
		return null;
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {

	}

	@Override
	public void setLastEditedTimestamp() {

	}

	@Override
	public String getLastEditedDate() {
		return null;
	}

	@Override
	public void setCreator(HibUser user) {

	}

	@Override
	public void setCreated(HibUser creator) {

	}

	@Override
	public void setCreationTimestamp(long timestamp) {

	}

	@Override
	public void setCreationTimestamp() {

	}

	@Override
	public Long getCreationTimestamp() {
		return null;
	}

	@Override
	public Object getId() {
		return null;
	}

	@Override
	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
		return Collections.emptySet();
	}

	@Override
	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
	}

	@Override
	public TypeInfo getTypeInfo() {
		return null;
	}

	@Override
	public String getElementVersion() {
		return null;
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return null;
	}

	@Override
	public void setCachedUuid(String userUuid) {

	}

}
