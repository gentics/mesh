package com.gentics.cailun.core.data.service.generic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.repository.generic.GenericFileRepository;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;

@Component
@Transactional
public class GenericFileServiceImpl<T extends GenericFile> extends GenericPropertyContainerServiceImpl<T> implements GenericFileService<T> {

	@Autowired
	private CaiLunSpringConfiguration springConfig;

	// @Autowired
	// private ProjectService projectService;

	@Autowired
	private GenericFileRepository<T> fileRepository;

	@Autowired
	private Neo4jTemplate template;

	public void setFilename(T file, Language language, String filename) throws UnsupportedOperationException {
		// TODO check for conflicting i18n filenames
		setProperty(file, language, GenericFile.FILENAME_KEYWORD, filename);
	}

	@Override
	public T findByPath(String projectName, String path) {
		// Delegate to project repository since this request is project specific
		return (T) fileRepository.findByProjectPath(projectName, path);
	}

	
}
