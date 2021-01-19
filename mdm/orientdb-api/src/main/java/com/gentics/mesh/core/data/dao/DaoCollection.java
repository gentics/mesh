package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.action.BranchDAOActions;
import com.gentics.mesh.core.action.GroupDAOActions;
import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.action.ProjectDAOActions;
import com.gentics.mesh.core.action.RoleDAOActions;
import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.action.UserDAOActions;

/**
 * Aggregated collection of DAOs
 */
public interface DaoCollection {

	UserDaoWrapper userDao();

	UserDAOActions userActions();

	GroupDaoWrapper groupDao();

	GroupDAOActions groupActions();

	RoleDaoWrapper roleDao();

	RoleDAOActions roleActions();

	ProjectDaoWrapper projectDao();

	ProjectDAOActions projectActions();

	LanguageDaoWrapper languageDao();

	JobDaoWrapper jobDao();

	TagFamilyDaoWrapper tagFamilyDao();

	TagFamilyDAOActions tagFamilyActions();

	TagDaoWrapper tagDao();

	TagDAOActions tagActions();

	BranchDaoWrapper branchDao();

	BranchDAOActions branchActions();

	MicroschemaDaoWrapper microschemaDao();

	MicroschemaDAOActions microschemaActions();

	SchemaDaoWrapper schemaDao();

	SchemaDAOActions schemaActions();

	BinaryDaoWrapper binaryDao();

	NodeDaoWrapper nodeDao();

	ContentDaoWrapper contentDao();

}
