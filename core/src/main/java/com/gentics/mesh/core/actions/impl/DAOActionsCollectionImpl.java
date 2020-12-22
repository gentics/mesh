package com.gentics.mesh.core.actions.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.DAOActionsCollection;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.NodeDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;
import com.gentics.mesh.core.data.dao.DaoCollection;

/**
 * Daggerized implementation of a {@link DaoCollection}
 */
@Singleton
public class DAOActionsCollectionImpl implements DAOActionsCollection {

	private final UserDAOActions userActions;
	private final GroupDAOActions groupActions;
	private final RoleDAOActions roleActions;
	private final TagDAOActions tagActions;
	private final TagFamilyDAOActions tagFamilyActions;
	private final BranchDAOActions branchActions;
	private final ProjectDAOActions projectActions;
	private final NodeDAOActions nodeActions;
	private final MicroschemaDAOActions microschemaActions;
	private final SchemaDAOActions schemaActions;

	@Inject
	public DAOActionsCollectionImpl(
		UserDAOActions userActions,
		GroupDAOActions groupActions,
		RoleDAOActions roleActions,
		TagDAOActions tagActions,
		TagFamilyDAOActions tagFamilyActions,
		BranchDAOActions branchActions,
		ProjectDAOActions projectActions,
		NodeDAOActions nodeActions,
		MicroschemaDAOActions microschemaActions,
		SchemaDAOActions schemaActions) {

		this.userActions = userActions;
		this.groupActions = groupActions;
		this.roleActions = roleActions;

		this.tagActions = tagActions;
		this.tagFamilyActions = tagFamilyActions;

		this.branchActions = branchActions;
		this.projectActions = projectActions;
		this.nodeActions = nodeActions;

		this.microschemaActions = microschemaActions;
		this.schemaActions = schemaActions;
	}

	@Override
	public UserDAOActions userActions() {
		return userActions;
	}

	@Override
	public GroupDAOActions groupActions() {
		return groupActions;
	}

	@Override
	public RoleDAOActions roleActions() {
		return roleActions;
	}

	@Override
	public TagDAOActions tagActions() {
		return tagActions;
	}

	@Override
	public TagFamilyDAOActions tagFamilyActions() {
		return tagFamilyActions;
	}

	@Override
	public BranchDAOActions branchActions() {
		return branchActions;
	}

	@Override
	public ProjectDAOActions projectActions() {
		return projectActions;
	}

	@Override
	public NodeDAOActions nodeActions() {
		return nodeActions;
	}

	@Override
	public MicroschemaDAOActions microschemaActions() {
		return microschemaActions;
	}

	@Override
	public SchemaDAOActions schemaActions() {
		return schemaActions;
	}

}
