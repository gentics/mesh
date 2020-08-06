package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.LanguageRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.SchemaContainerRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.root.UserRoot;

public interface TxData {
	UserRoot userDao();

	GroupRoot groupDao();

	RoleRoot roleDao();

	ProjectRoot projectDao();

	LanguageRoot languageDao();

	JobRoot jobDao();

	TagFamilyRoot tagFamilyDao();

	TagRoot tagDao();

	MicroschemaContainerRoot microschemaDao();

	SchemaContainerRoot schemaDao();

}
