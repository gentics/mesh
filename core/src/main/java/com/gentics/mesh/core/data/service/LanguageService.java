package com.gentics.mesh.core.data.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.impl.LanguageImpl;
import com.gentics.mesh.core.data.model.impl.TagImpl;

@Component
public class LanguageService extends AbstractMeshGraphService<Language> {

	public static LanguageService instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static LanguageService getLanguageService() {
		return instance;
	}

	public Language findByName(String name) {
		return findByName(name, LanguageImpl.class);
	}

	@Override
	public List<? extends Language> findAll() {
		return fg.v().has(LanguageImpl.class).toListExplicit(LanguageImpl.class);
	}

	/**
	 * Find the language with the specified http://en.wikipedia.org/wiki/IETF_language_tag[IETF language tag].
	 * 
	 * @param languageTag
	 * @return Found language or null if none could be found
	 */
	public Language findByLanguageTag(String languageTag) {
		return fg.v().has("languageTag", languageTag).has(LanguageImpl.class).nextOrDefault(LanguageImpl.class, null);
	}

	/**
	 * The tag language is currently fixed to english since we only want to store tags based on a single language. The idea is that tags will be localizable in
	 * the future.
	 */
	public Language getTagDefaultLanguage() {
		return findByLanguageTag(TagImpl.DEFAULT_TAG_LANGUAGE_TAG);
	}

	@Override
	public Language findByUUID(String uuid) {
		return findByUUID(uuid, LanguageImpl.class);
	}
}
