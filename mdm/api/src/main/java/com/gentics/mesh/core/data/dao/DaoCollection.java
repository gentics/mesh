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

	S3BinaryDao s3binaryDao();

	NodeDao nodeDao();

	ContentDao contentDao();

}
