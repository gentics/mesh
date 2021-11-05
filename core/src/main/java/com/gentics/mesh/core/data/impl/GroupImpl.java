package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.util.Set;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.field.FieldType;

/**
 * @see Group
 */
public class GroupImpl extends AbstractMeshCoreVertex<GroupResponse> implements Group {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(GroupImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(GroupImpl.class)
			.withField("name", FieldType.STRING)
			.unique());
	}

	@Override
	public GroupReference transformToReference() {
		return new GroupReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	public String getName() {
		return property("name");
	}

	@Override
	public void setName(String name) {
		property("name", name);
	}

	@Override
	public void delete(BulkActionContext bac) {
		GroupDao groupRoot = Tx.get().groupDao();
		groupRoot.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead.");
	}

	@Override
	public boolean applyPermissions(EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		GroupDao groupDao = Tx.get().groupDao();
		boolean permissionChanged = false;
		if (recursive) {
			for (HibUser user : groupDao.getUsers(this)) {
				User graphUser = toGraph(user);
				permissionChanged = graphUser.applyPermissions(batch, role, false, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = super.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}

	@Override
	public HibUser getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public void removeElement() {
		getElement().remove();
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}

	@Override
	public void generateBucketId() {
		BucketableElementHelper.generateBucketId(this);
	}
}
