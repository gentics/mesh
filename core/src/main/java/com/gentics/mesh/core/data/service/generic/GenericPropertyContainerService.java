package com.gentics.mesh.core.data.service.generic;

import com.gentics.mesh.core.data.model.generic.GenericPropertyContainer;
import com.gentics.mesh.core.data.model.tinkerpop.I18NProperties;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshNode;
import com.gentics.mesh.core.data.model.tinkerpop.Translated;
import com.gentics.mesh.core.repository.action.PropertyContainerActions;

public interface GenericPropertyContainerService<T extends GenericPropertyContainer> extends GenericNodeService<T>, PropertyContainerActions<T> {

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
