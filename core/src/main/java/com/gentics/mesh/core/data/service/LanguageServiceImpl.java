package com.gentics.mesh.core.data.service;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

@Component
public class LanguageServiceImpl extends AbstractMeshService implements LanguageService {

	public Language findByName(String name) {
		return framedGraph.V().has("name", name).has("java_class", Language.class).next(Language.class);
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag) {
		return framedGraph.V().has("languageTag", languageTag).next(Language.class);
	}

	//	@Override
	//	public Language save(Language language) {
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
	//		return null;
	//	}

	public LanguageRoot findRoot() {
		return framedGraph.V().has("java_class", LanguageRoot.class.getName()).next(LanguageRoot.class);
	}

	@Override
	public LanguageRoot createRoot() {
		LanguageRoot root = framedGraph.addVertex(LanguageRoot.class);
		return root;
	}

	@Override
	public Language create(String languageName, String languageTag) {
		Language language = framedGraph.addVertex(Language.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		return language;
	}
}
