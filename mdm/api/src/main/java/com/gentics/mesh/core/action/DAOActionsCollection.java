package com.gentics.mesh.core.action;

import com.gentics.mesh.annotation.Getter;

/**
 * Collection of all DAO actions which can be used in a REST or GraphQL context.
 */
public interface DAOActionsCollection {

	@Getter
	UserDAOActions userActions();

	@Getter
	GroupDAOActions groupActions();

	@Getter
	RoleDAOActions roleActions();

	@Getter
	TagDAOActions tagActions();

	@Getter
	TagFamilyDAOActions tagFamilyActions();

	@Getter
	BranchDAOActions branchActions();

	@Getter
	ProjectDAOActions projectActions();

	@Getter
	NodeDAOActions nodeActions();

	@Getter
	SchemaDAOActions schemaActions();

	@Getter
	MicroschemaDAOActions microschemaActions();

}
