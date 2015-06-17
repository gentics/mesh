package com.gentics.mesh.core.data.service;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.root.LanguageRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.util.TraversalHelper;

@Component
public class LanguageService extends AbstractMeshService {

	public Language findByName(String name) {
		return TraversalHelper.nextExplicitOrNull(fg.v().has("name", name).has(Language.class), Language.class);
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag) {
		return TraversalHelper.nextExplicitOrNull(fg.v().has("languageTag", languageTag).has(Language.class), Language.class);
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
		return TraversalHelper.nextExplicitOrNull(fg.v().has(LanguageRoot.class), LanguageRoot.class);
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
