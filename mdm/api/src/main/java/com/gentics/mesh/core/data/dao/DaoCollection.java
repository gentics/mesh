package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.action.BranchDAOActions;
import com.gentics.mesh.core.data.action.GroupDAOActions;
import com.gentics.mesh.core.data.action.MicroschemaDAOActions;
import com.gentics.mesh.core.data.action.ProjectDAOActions;
import com.gentics.mesh.core.data.action.RoleDAOActions;
import com.gentics.mesh.core.data.action.SchemaDAOActions;
import com.gentics.mesh.core.data.action.TagDAOActions;
import com.gentics.mesh.core.data.action.TagFamilyDAOActions;
import com.gentics.mesh.core.data.action.UserDAOActions;

/* 
 * Aggregated collection of DAOs
 */
public interface DaoCollection {

	UserDao userDao();

	UserDAOActions userActions();

	GroupDao groupDao();

	GroupDAOActions groupActions();

	RoleDao roleDao();

	RoleDAOActions roleActions();

	ProjectDao projectDao();

	ProjectDAOActions projectActions();

	LanguageDao languageDao();

	JobDao jobDao();

	TagFamilyDao tagFamilyDao();

	TagFamilyDAOActions tagFamilyActions();

	TagDao tagDao();

	TagDAOActions tagActions();

	BranchDao branchDao();

	BranchDAOActions branchActions();

	MicroschemaDao microschemaDao();

	MicroschemaDAOActions microschemaActions();

	SchemaDao schemaDao();

	SchemaDAOActions schemaActions();

	BinaryDao binaryDao();

	NodeDao nodeDao();

	ContentDao contentDao();

}
