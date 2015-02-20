package com.gentics.cailun.core.repository.generic;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.ProjectRepository;
import com.gentics.cailun.core.repository.action.GenericFileRepositoryActions;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

public class GenericFileRepositoryImpl<T extends GenericFile> extends GenericPropertyContainerRepositoryImpl<T> implements GenericFileRepositoryActions<T> {
	
	@Autowired
	private ProjectRepository projectRepository;

	public GenericFile findByProjectPath(String projectName, String path){
		return projectRepository.findFileByPath(projectName, path);
	}

}
