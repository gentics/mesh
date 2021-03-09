package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandlerImpl;
import com.gentics.mesh.madl.field.FieldType;

/**
 * @see Role
 */
public class RoleImpl extends AbstractMeshCoreVertex<RoleResponse> implements Role {

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(RoleImpl.class, MeshVertexImpl.class);
		index.createIndex(vertexIndex(RoleImpl.class)
			.withField("name", FieldType.STRING)
			.unique());
	}

	@Override
	public RoleReference transformToReference() {
		return new RoleReference().setName(getName()).setUuid(getUuid());
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
	public Result<? extends Group> getGroups() {
		return out(HAS_ROLE, GroupImpl.class);
	}

	@Override
	public RoleResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		RoleDao roleDao = mesh().boot().roleDao();
		return roleDao.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public void delete(BulkActionContext bac) {
		RoleDao roleDao = mesh().boot().roleDao();
		roleDao.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		RoleDao roleDao = mesh().boot().roleDao();
		return roleDao.update(this, ac, batch);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandlerImpl.baseRoute(ac) + "/roles/" + getUuid();
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
