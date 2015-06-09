package com.gentics.mesh.core.data.service;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.service.generic.GenericNodeServiceImpl;

@Component
public class LanguageServiceImpl extends GenericNodeServiceImpl<Language> implements LanguageService {

	@Override
	public Language findByName(String name) {
		return null;
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	@Override
	public Language findByLanguageTag(String languageTag) {
		return null;
	}

	@Override
	public Language save(Language language) {
		//		if (StringUtils.isEmpty(language.getLanguageTag()) || StringUtils.isEmpty(language.getName())) {
		//			// TODO throw exception?
		//		}
		//		LanguageRoot root = findRoot();
		//		if (root == null) {
		//			throw new NullPointerException("The language root node could not be found.");
		//		}
		//		language = neo4jTemplate.save(language);
		//		root.getLanguages().add(language);
		//		neo4jTemplate.save(root);
		//		return language;
		return null;
	}

	public LanguageRoot findRoot() {
		//		@Query("MATCH (n:LanguageRoot) return n")
		return null;
	}

	@Override
	public LanguageRoot createRoot() {
		LanguageRoot root = framedGraph.addVertex(null, LanguageRoot.class);
		return root;
	}

	@Override
	public Language create(String languageName, String languageTag) {
		Language language = framedGraph.addVertex(null, Language.class);
		language.setName(languageTag);
		language.setNativeName(languageName);
		return language;
	}
}
