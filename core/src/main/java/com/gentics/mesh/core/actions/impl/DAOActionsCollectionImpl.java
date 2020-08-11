package com.gentics.mesh.core.actions.impl;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.actions.BranchDAOActions;
import com.gentics.mesh.core.actions.DAOActionsCollection;
import com.gentics.mesh.core.actions.GroupDAOActions;
import com.gentics.mesh.core.actions.MicroschemaDAOActions;
import com.gentics.mesh.core.actions.NodeDAOActions;
import com.gentics.mesh.core.actions.ProjectDAOActions;
import com.gentics.mesh.core.actions.RoleDAOActions;
import com.gentics.mesh.core.actions.SchemaDAOActions;
import com.gentics.mesh.core.actions.TagDAOActions;
import com.gentics.mesh.core.actions.TagFamilyDAOActions;
import com.gentics.mesh.core.actions.UserDAOActions;

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
