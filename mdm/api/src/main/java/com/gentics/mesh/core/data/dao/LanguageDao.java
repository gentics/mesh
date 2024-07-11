package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for language.
 */
public interface LanguageDao extends DaoGlobal<Language>, DaoTransformable<Language, LanguageResponse> {

	/**
	 * Return the language for the given language tag.
	 * 
	 * @param tag
	 * @return
	 */
	Language findByLanguageTag(String tag);

	/**
	 * Return the project language for the given language tag.
	 * 
	 * @param tag
	 * @return
	 */
	Language findByLanguageTag(Project project, String tag);

	/**
	 * Create the language.
	 * 
	 * @param languageName
	 * @param languageTag
	 * @return
	 */
	Language create(String languageName, String languageTag);

	/**
	 * Assign the language to the project.
	 * 
	 * @param language
	 * @param project
	 * @param batch
	 */
	void assign(Language language, Project project, EventQueueBatch batch, boolean throwOnExisting);

	/**
	 * Remove the language from the project.
	 * 
	 * @param language
	 * @param project
	 * @param batch
	 */
	void unassign(Language language, Project project, EventQueueBatch batch, boolean throwOnInexisting);
}
