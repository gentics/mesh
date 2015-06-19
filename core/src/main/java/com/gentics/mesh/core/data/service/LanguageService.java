package com.gentics.mesh.core.data.service;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;

@Component
public class LanguageService extends AbstractMeshService {

	public static LanguageService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static LanguageService getLanguageService() {
		return instance;
	}

	
	public Language findByName(String name) {
		return fg.v().has("name", name).nextOrDefault(Language.class,null);
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag) {
		return fg.v().has("languageTag", languageTag).nextOrDefault(Language.class, null);
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
		return fg.v().nextOrDefault(LanguageRoot.class, null);
	}

	public LanguageRoot createRoot() {
		LanguageRoot root = fg.addFramedVertex(LanguageRoot.class);
		return root;
	}

	public Language create(String languageName, String languageTag) {
		Language language = fg.addFramedVertex(Language.class);
		language.setName(languageName);
		language.setLanguageTag(languageTag);
		LanguageRoot root = findRoot();
		root.addLanguage(language);
		return language;
	}
}
