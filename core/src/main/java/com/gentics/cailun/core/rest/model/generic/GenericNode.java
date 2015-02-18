package com.gentics.cailun.core.rest.model.generic;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import com.gentics.cailun.core.rest.model.I18NProperties;
import com.gentics.cailun.core.rest.model.Language;
import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;
import com.gentics.cailun.core.rest.model.relationship.Locked;
import com.gentics.cailun.core.rest.model.relationship.Translated;

/**
 * This class represents a basic cailun node. All models that make use of this model will automatically be able to be handled by the permission system.
 * 
 * @author johannes2
 *
 */
@Data
@NodeEntity
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;
	public static final String NAME_KEYWORD = "name";

	@RelatedTo(type = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	protected Project project;

	@RelatedTo(type = BasicRelationships.HAS_CREATOR, direction = Direction.OUTGOING, elementClass = User.class)
	protected User creator;

	@RelatedToVia(type = AuthRelationships.HAS_PERMISSION, direction = Direction.INCOMING, elementClass = GraphPermission.class)
	protected Set<GraphPermission> permissions = new HashSet<>();

	protected DynamicProperties properties = new DynamicPropertiesContainer();

	@Fetch
	@RelatedToVia(type = BasicRelationships.IS_LOCKED, direction = Direction.OUTGOING, elementClass = Locked.class)
	protected Locked locked;

	@Fetch
	@RelatedToVia(type = BasicRelationships.HAS_I18NVALUE, direction = Direction.OUTGOING, elementClass = Translated.class)
	protected Set<Translated> i18nTranslations = new HashSet<>();

	/**
	 * Returns the i18n specific text value for the given language.
	 * 
	 * @param language
	 * @param key
	 * @return the found text value or null if no value could be found
	 */
	protected String getI18NProperty(Language language, String key) {
		if (language == null || StringUtils.isEmpty(key)) {
			return null;
		}
		return getProperty(language, key);
	}

	public String getName(Language language) {
		return getI18NProperty(language, NAME_KEYWORD);
	}

	/**
	 * Adds a new property or updates an exiting property with the given key and value.
	 * 
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) {
		this.properties.setProperty(key, value);
	}

	/**
	 * Returns the value for the given key.
	 * 
	 * @param key
	 * @return null, when the value could not be found
	 */
	public String getProperty(String key) {
		return (String) properties.getProperty(key);
	}

	/**
	 * Removes the property with the given key.
	 * 
	 * @param key
	 * @return true, when the property could be removed. Otherwise false.
	 */
	public boolean removeProperty(String key) {
		if (properties.removeProperty(key) == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Checks whether the property with the given key exists.
	 * 
	 * @param key
	 * @return true, when the key was found. Otherwise false.
	 */
	public boolean hasProperty(String key) {
		return properties.hasProperty(key);
	}

	public I18NProperties getI18NValue(Language language) {
		for (Translated translation : i18nTranslations) {
			if (translation.getLanguageTag().equalsIgnoreCase(language.getLanguageTag())) {
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

	public boolean addPermission(GraphPermission permission) {
		return permissions.add(permission);
	}

}