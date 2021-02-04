package com.gentics.mesh.core.data.action;

/**
 * Collection of all DAO actions which can be used in a REST or GraphQL context.
 */
public interface DAOActionsCollection {

	UserDAOActions userActions();

	GroupDAOActions groupActions();

	RoleDAOActions roleActions();

	TagDAOActions tagActions();

	TagFamilyDAOActions tagFamilyActions();

	BranchDAOActions branchActions();

	ProjectDAOActions projectActions();

	NodeDAOActions nodeActions();

	SchemaDAOActions schemaActions();

	MicroschemaDAOActions microschemaActions();

}
