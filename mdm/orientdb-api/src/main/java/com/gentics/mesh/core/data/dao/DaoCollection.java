package com.gentics.mesh.core.data.dao;

public interface DaoCollection {

	UserDaoWrapper userDao();

	GroupDaoWrapper groupDao();

	RoleDaoWrapper roleDao();

	ProjectDaoWrapper projectDao();

	LanguageDaoWrapper languageDao();

	JobDaoWrapper jobDao();

	TagFamilyDaoWrapper tagFamilyDao();

	TagDaoWrapper tagDao();

	BranchDaoWrapper branchDao();

	MicroschemaDaoWrapper microschemaDao();

	SchemaDaoWrapper schemaDao();

	BinaryDaoWrapper binaryDao();

}
