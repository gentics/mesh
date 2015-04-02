package com.gentics.cailun.core.data.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import com.gentics.cailun.core.data.model.I18NProperties;
import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.data.model.relationship.Translated;

@NodeEntity
public class GenericPropertyContainer extends GenericNode {

	private static final long serialVersionUID = 7551202734708358487L;

	public static final String NAME_KEYWORD = "name";

	@Fetch
	@RelatedToVia(type = BasicRelationships.HAS_I18N_PROPERTIES, direction = Direction.OUTGOING, elementClass = Translated.class)
	protected Set<Translated> i18nTranslations = new HashSet<>();

	public String getName(Language language) {
		return getProperty(language, NAME_KEYWORD);
	}

	public I18NProperties getI18NProperties(Language language) {
		if (language == null) {
			return null;
		}
		for (Translated translation : i18nTranslations) {
			if (translation.getLanguageTag() == null) {
				continue;
			}
			if (translation.getLanguageTag().equals(language.getLanguageTag())) {
				return translation.getI18nValue();
			}
		}
		return null;
	}

	/**
	 * Returns the i18n value for the given language and key.
	 * 
	 * @param language
	 * @param key
	 * @return the found i18n string or null if no value could be found.
	 */
	public String getProperty(Language language, String key) {
		if (language == null || StringUtils.isEmpty(key)) {
			return null;
		}
		for (Translated translation : i18nTranslations) {
			if (translation.getLanguageTag().equalsIgnoreCase(language.getLanguageTag())) {
				I18NProperties value = translation.getI18nValue();
				return value.getProperty(key);
			}
		}
		return null;
	}

	public Set<Translated> getI18nTranslations() {
		return i18nTranslations;
	}

}
