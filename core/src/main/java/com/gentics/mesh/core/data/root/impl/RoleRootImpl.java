package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;

import org.apache.commons.lang3.NotImplementedException;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.root.RoleRoot;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class RoleRootImpl extends AbstractRootVertex<Role>implements RoleRoot {

	private static final Logger log = LoggerFactory.getLogger(RoleRootImpl.class);

	@Override
	protected Class<? extends Role> getPersistanceClass() {
		return RoleImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_ROLE;
	}

	@Override
	public void addRole(Role role) {
		if (log.isDebugEnabled()) {
			log.debug("Adding role {" + role.getUuid() + ":" + role.getName() + "#" + role.getImpl().getId() + "} to roleRoot {" + getId() + "}");
		}
		addItem(role);
	}

	@Override
	public void removeRole(Role role) {
		// TODO delete the role? unlink from all groups? how is ferma / blueprint handling this. Neo4j would explode when trying to remove a node that still has
		// connecting edges.
		removeItem(role);
	}

	@Override
	public Role create(String name, Group group, User creator) {
		Role role = getGraph().addFramedVertex(RoleImpl.class);
		role.setName(name);
		role.setCreator(creator);
		role.setCreationTimestamp(System.currentTimeMillis());
		role.setEditor(creator);
		role.setLastEditedTimestamp(System.currentTimeMillis());
		addRole(role);
		if (group != null) {
			group.addRole(role);
		}
		return role;
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The role root node can't be deleted");
	}

}
