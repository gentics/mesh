package com.gentics.mesh.core.data.impl;

import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * Wraps a {@link HibUser} to implement {@link MeshAuthUser}.
 * 
 * @see MeshAuthUser
 */
public class MeshAuthUserImpl implements MeshAuthUser {

	private final Database db;
	private final HibUser delegate;

	private MeshAuthUserImpl(Database db, HibUser user) {
		this.db = db;
		this.delegate = user;
	}

	public static MeshAuthUserImpl create(Database db, HibUser user) {
		Objects.requireNonNull(db);
		if (user == null) {
			return null;
		}
		return new MeshAuthUserImpl(db, user);
	}

	/**
	 * An active transaction is required in order to load the json data.
	 */
	@Override
	public JsonObject principal() {
		return db.tx(tx -> {
			UserDaoWrapper userDao = tx.userDao();
			JsonObject user = new JsonObject();
			user.put("uuid", getUuid());
			user.put("username", getUsername());
			user.put("firstname", getFirstname());
			user.put("lastname", getLastname());
			user.put("emailAddress", getEmailAddress());
			user.put("admin", isAdmin());

			JsonArray rolesArray = new JsonArray();
			user.put("roles", rolesArray);
			for (HibRole role : userDao.getRoles(delegate)) {
				JsonObject roleJson = new JsonObject();
				roleJson.put("uuid", role.getUuid());
				roleJson.put("name", role.getName());
				rolesArray.add(roleJson);
			}

			JsonArray groupsArray = new JsonArray();
			user.put("groups", groupsArray);
			for (HibGroup group : userDao.getGroups(delegate)) {
				JsonObject groupJson = new JsonObject();
				groupJson.put("uuid", group.getUuid());
				groupJson.put("name", group.getName());
				groupsArray.add(groupJson);
			}

			HibNode reference = delegate.getReferencedNode();
			if (reference != null) {
				user.put("nodeReference", reference.getUuid());
			}
			return user;
		});
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		throw new NotImplementedException();
	}

	@Override
	public User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Please use the MeshAuthUserImpl method instead.");
	}

	@Override
	public User clearCache() {
		throw new NotImplementedException();
	}

	@Override
	public void writeToBuffer(Buffer buffer) {
		throw new NotImplementedException();
	}

	@Override
	public int readFromBuffer(int pos, Buffer buffer) {
		throw new NotImplementedException();
	}

	@Override
	public String getUsername() {
		return delegate.getUsername();
	}

	@Override
	public HibUser setUsername(String string) {
		return delegate.setUsername(string);
	}

	@Override
	public String getLastname() {
		return delegate.getLastname();
	}

	@Override
	public HibUser setLastname(String lastname) {
		return delegate.setLastname(lastname);
	}

	@Override
	public String getFirstname() {
		return delegate.getFirstname();
	}

	@Override
	public HibUser setFirstname(String firstname) {
		return delegate.setFirstname(firstname);
	}

	@Override
	public String getEmailAddress() {
		return delegate.getEmailAddress();
	}

	@Override
	public HibUser setEmailAddress(String email) {
		return delegate.setEmailAddress(email);
	}

	@Override
	public String getPasswordHash() {
		return delegate.getPasswordHash();
	}

	@Override
	public HibUser disable() {
		return delegate.disable();
	}

	@Override
	public boolean isEnabled() {
		return delegate.isEnabled();
	}

	@Override
	public HibUser enable() {
		return delegate.enable();
	}

	@Override
	public UserReference transformToReference() {
		return delegate.transformToReference();
	}

	@Override
	public Long getResetTokenIssueTimestamp() {
		return delegate.getResetTokenIssueTimestamp();
	}

	@Override
	public HibUser invalidateResetToken() {
		return delegate.invalidateResetToken();
	}

	@Override
	public HibUser setPasswordHash(String hash) {
		return delegate.setPasswordHash(hash);
	}

	@Override
	public boolean isForcedPasswordChange() {
		return delegate.isForcedPasswordChange();
	}

	@Override
	public HibUser setForcedPasswordChange(boolean force) {
		return delegate.setForcedPasswordChange(force);
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
	public String getAPIKeyTokenCode() {
		return delegate.getAPIKeyTokenCode();
	}

	@Override
	public Long getAPITokenIssueTimestamp() {
		return delegate.getAPITokenIssueTimestamp();
	}

	@Override
	public void resetAPIToken() {
		delegate.resetAPIToken();
	}

	@Override
	public HibUser setResetToken(String token) {
		return delegate.setResetToken(token);
	}

	@Override
	public HibUser setAPITokenId(String code) {
		return delegate.setAPITokenId(code);
	}

	@Override
	public HibUser setResetTokenIssueTimestamp(Long timestamp) {
		return delegate.setResetTokenIssueTimestamp(timestamp);
	}

	@Override
	public HibUser setAPITokenIssueTimestamp() {
		return delegate.setAPITokenIssueTimestamp();
	}

	@Override
	public String getAPITokenIssueDate() {
		return delegate.getAPITokenIssueDate();
	}

	@Override
	public String getResetToken() {
		return delegate.getResetToken();
	}

	@Override
	public void updateShortcutEdges() {
		delegate.updateShortcutEdges();
	}

	@Override
	@Deprecated
	public void remove() {
		delegate.remove();
	}

	@Override
	public HibNode getReferencedNode() {
		return delegate.getReferencedNode();
	}

	@Override
	public HibUser setReferencedNode(HibNode node) {
		return delegate.setReferencedNode(node);
	}

	@Override
	public String getRolesHash() {
		return delegate.getRolesHash();
	}

	@Override
	public String getElementVersion() {
		return delegate.getElementVersion();
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return delegate.toAuthUser();
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
	public TypeInfo getTypeInfo() {
		return delegate.getTypeInfo();
	}

	@Override
	public String getUuid() {
		return delegate.getUuid();
	}

	@Override
	public Object getId() {
		return delegate.getId();
	}

	@Override
	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
		delegate.setRoleUuidForPerm(permission, allowedRoles);
	}

	@Override
	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	@Override
	public HibUser getEditor() {
		return delegate.getEditor();
	}

	@Override
	public void setEditor(HibUser user) {
		delegate.setEditor(user);
	}

	@Override
	public Long getLastEditedTimestamp() {
		return delegate.getLastEditedTimestamp();
	}

	@Override
	public String getLastEditedDate() {
		return delegate.getLastEditedDate();
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
	public HibUser getCreator() {
		return delegate.getCreator();
	}

	@Override
	public void setCreator(HibUser user) {
		delegate.setCreator(user);
	}

	@Override
	public void setCreated(HibUser creator) {
		delegate.setCreated(creator);
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
	public String getCreationDate() {
		return delegate.getCreationDate();
	}

	@Override
	public Long getCreationTimestamp() {
		return delegate.getCreationTimestamp();
	}

	public HibUser getDelegate() {
		return delegate;
	}

}