package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.Map;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

import io.reactivex.Completable;

/**
 * Interface for post processing actions, which are performed asynchronously after form data has been published.
 *
 * <p>Implementations must be annotated with {@link PostProcessing}, must have a public no-argument constructor and
 * must be located in the package {@code com.gentics.mesh.plugin.forms.postprocessing}.
 * One instance per implementation is created when the plugin is started, so implementations must be thread safe.
 */
public interface PostProcessingAction {
	/**
	 * Check whether the post processing action must be performed for the published form data.
	 * This method is called synchronously while the form data is published.
	 *
	 * @param properties properties of the post processing configuration (empty map, if nothing is configured)
	 * @param form form definition
	 * @param postedData posted form data
	 * @param language language of the posted form data
	 * @return true iff the action must be performed
	 */
	boolean shouldProcess(Map<String, Object> properties, Form form, PostedData postedData, String language);

	/**
	 * Perform the post processing action. This method is called asynchronously after the form data has been published.
	 * If the returned completable fails (or this method throws), the action will be retried, until the configured maximum number of attempts is reached.
	 *
	 * @param properties properties of the post processing configuration (empty map, if nothing is configured)
	 * @param form form definition
	 * @param postedData posted form data
	 * @param language language of the posted form data
	 * @return completable
	 */
	Completable process(Map<String, Object> properties, Form form, PostedData postedData, String language);
}
