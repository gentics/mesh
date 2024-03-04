package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for language.
 */
public interface LanguageDao extends DaoGlobal<HibLanguage>, DaoTransformable<HibLanguage, LanguageResponse> {

	/**
	 * Return the language for the given language tag.
	 * 
	 * @param tag
	 * @return
	 */
	HibLanguage findByLanguageTag(String tag);

	/**
	 * Return the project language for the given language tag.
	 * 
	 * @param tag
	 * @return
	 */
	HibLanguage findByLanguageTag(HibProject project, String tag);

	/**
	 * Create the language.
	 * 
	 * @param languageName
	 * @param languageTag
	 * @return
	 */
	HibLanguage create(String languageName, String languageTag);

	/**
	 * Assign the language to the project.
	 * 
	 * @param language
	 * @param project
	 * @param batch
	 */
	void assign(HibLanguage language, HibProject project, EventQueueBatch batch, boolean throwOnExisting);

	/**
	 * Remove the language from the project.
	 * 
	 * @param language
	 * @param project
	 * @param batch
	 */
	void unassign(HibLanguage language, HibProject project, EventQueueBatch batch, boolean throwOnInexisting);
}
