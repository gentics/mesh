package com.gentics.mesh.core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.LanguageRoot;
import com.gentics.mesh.core.repository.action.LanguageActions;
import com.gentics.mesh.core.repository.generic.GenericNodeRepositoryImpl;

public class LanguageRepositoryImpl extends GenericNodeRepositoryImpl<Language> implements LanguageActions {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	@Autowired
	private LanguageRepository languageRepository;

	@Override
	public Language save(Language language) {
		LanguageRoot root = languageRepository.findRoot();
		if (root == null) {
			throw new NullPointerException("The language root node could not be found.");
		}
		language = neo4jTemplate.save(language);
		root.getLanguages().add(language);
		neo4jTemplate.save(root);
		return language;
	}

}
