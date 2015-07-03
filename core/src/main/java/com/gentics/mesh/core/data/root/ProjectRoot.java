package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;

public interface ProjectRoot extends RootVertex<Project> {

	Project create(String projectName);

	void addProject(Project project);

	ProjectRootImpl getImpl();

	Page<? extends Project> findAllVisible(User requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

}
