package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;

public interface TxData {

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
