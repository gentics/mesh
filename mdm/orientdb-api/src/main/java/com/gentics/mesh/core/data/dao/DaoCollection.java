package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.annotation.Getter;
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
	
	@Getter
	UserDaoWrapper userDao();

	@Getter
	UserDAOActions userActions();

	@Getter
	GroupDaoWrapper groupDao();

	@Getter
	GroupDAOActions groupActions();

	@Getter
	RoleDaoWrapper roleDao();

	@Getter
	RoleDAOActions roleActions();

	@Getter
	ProjectDaoWrapper projectDao();

	@Getter
	ProjectDAOActions projectActions();

	@Getter
	LanguageDaoWrapper languageDao();

	@Getter
	JobDaoWrapper jobDao();

	@Getter
	TagFamilyDaoWrapper tagFamilyDao();

	@Getter
	TagFamilyDAOActions tagFamilyActions();

	@Getter
	TagDaoWrapper tagDao();

	@Getter
	TagDAOActions tagActions();

	@Getter
	BranchDaoWrapper branchDao();

	@Getter
	BranchDAOActions branchActions();

	@Getter
	MicroschemaDaoWrapper microschemaDao();

	@Getter
	MicroschemaDAOActions microschemaActions();

	@Getter
	SchemaDaoWrapper schemaDao();

	@Getter
	SchemaDAOActions schemaActions();

	@Getter
	BinaryDaoWrapper binaryDao();

	@Getter
	NodeDaoWrapper nodeDao();

	@Getter
	ContentDaoWrapper contentDao();

}
