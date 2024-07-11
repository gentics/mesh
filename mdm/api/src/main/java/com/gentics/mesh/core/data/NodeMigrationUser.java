package com.gentics.mesh.core.data;

import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.user.UserReferenceModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * Create mocked user for node migrations.
 */
public class NodeMigrationUser implements User {

	@Override
	public UserReferenceModel transformToReference() {
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
	public Node getReferencedNode() {
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
	public User setUsername(String string) {
		return this;
	}

	@Override
	public User setLastname(String lastname) {
		// TODO Auto-generated method stub
		return null;
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
	public User disable() {
		return this;
	}

	@Override
	public User enable() {
		return this;
	}

	@Override
	public User invalidateResetToken() {
		return this;
	}

	@Override
	public User setPasswordHash(String hash) {
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
	public User setResetToken(String token) {
		return this;
	}

	@Override
	public User setReferencedNode(Node node) {
		return this;
	}

	@Override
	public void setEditor(User user) {

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
	public void setCreator(User user) {

	}

	@Override
	public void setCreated(User creator) {

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
	public void setBucketId(Integer bucketId) {
	}

	@Override
	public Integer getBucketId() {
		return null;
	}

	@Override
	public void generateBucketId() {
	}

	@Override
	public boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, Role role, boolean recursive,
                                    Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		return false;
	}

	@Override
	public void setUuid(String uuid) {		
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return null;
	}

	@Override
	public String getETag(InternalActionContext ac) {
		return null;
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return null;
	}

	@Override
	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		
	}

	@Override
	public String getName() {
		return getUsername();
	}

	@Override
	public void setName(String name) {
		setUsername(name);
	}
}
