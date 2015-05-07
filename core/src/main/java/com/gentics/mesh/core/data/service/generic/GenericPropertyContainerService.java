package com.gentics.mesh.core.data.service.generic;

import com.gentics.mesh.core.data.model.I18NProperties;
import com.gentics.mesh.core.data.model.Language;
import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;

public interface GenericPropertyContainerService<T extends GenericPropertyContainer> extends GenericNodeService<T> {

	/**
	 * Adds or updates the i18n value for the given language and key with the given value.
	 * 
	 * @param language
	 *            Language for the i18n value
	 * @param key
	 *            Key of the value
	 * @param value
	 *            The i18n text value
	 */
	public void setProperty(T node, Language language, String key, String value);

	public void setName(T node, Language language, String name);

	public void setContent(T node, Language language, String text);

	public void setFilename(T node, Language language, String filename);

	public String getName(T node, Language language);

	public String getContent(T node, Language language);

	public String getTeaser(T node, Language language);

	public String getTitle(T node, Language language);

	public String getFilename(T node, Language language);

	/**
	 * Returns the i18n value for the given language and key.
	 * 
	 * @param content
	 * @param language
	 * @param key
	 * @return the found i18n string or null if no value could be found.
	 */
	public String getProperty(T node, Language language, String key);

	public I18NProperties getI18NProperties(T node, Language language);

}
