package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.traversal.TraversalResult;

/**
 * @see Role
 */
public class RoleImpl extends AbstractMeshCoreVertex<RoleResponse, Role> implements Role {

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
	public TraversalResult<? extends Group> getGroups() {
		return out(HAS_ROLE, GroupImpl.class);
	}

	@Override
	public RoleResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		RoleDaoWrapper roleDao = mesh().boot().roleDao();
		return roleDao.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public void delete(BulkActionContext bac) {
		RoleDaoWrapper roleDao = mesh().boot().roleDao();
		roleDao.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		RoleDaoWrapper roleDao = mesh().boot().roleDao();
		return roleDao.update(this, ac, batch);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/roles/" + getUuid();
	}

	@Override
	public User getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

}
