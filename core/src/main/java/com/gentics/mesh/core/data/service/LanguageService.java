package com.gentics.mesh.core.data.service;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

@Component
public class LanguageService extends AbstractMeshService {

	public Language findByName(String name) {
		return framedGraph.v().has("name", name).has("ferma_type", Language.class).next(Language.class);
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag) {
		return framedGraph.v().has("languageTag", languageTag).next(Language.class);
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
		return framedGraph.v().has("ferma_type", LanguageRoot.class.getName()).next(LanguageRoot.class);
	}

	public LanguageRoot createRoot() {
		LanguageRoot root = framedGraph.addFramedVertex(LanguageRoot.class);
		return root;
	}

	public Language create(String languageName, String languageTag) {
		Language language = framedGraph.addFramedVertex(Language.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		return language;
	}
}
