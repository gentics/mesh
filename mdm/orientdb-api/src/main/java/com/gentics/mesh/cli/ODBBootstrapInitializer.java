package com.gentics.mesh.cli;

import com.gentics.mesh.core.data.changelog.ChangelogRoot;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.root.*;

public interface ODBBootstrapInitializer extends BootstrapInitializer {

	MeshRoot meshRoot();

	SchemaRoot schemaContainerRoot();

	MicroschemaRoot microschemaContainerRoot();

	RoleRoot roleRoot();

	TagRoot tagRoot();

	TagFamilyRoot tagFamilyRoot();

	ChangelogRoot changelogRoot();

	UserRoot userRoot();

	GroupRoot groupRoot();

	JobRoot jobRoot();

	LanguageRoot languageRoot();

	ProjectRoot projectRoot();
}
