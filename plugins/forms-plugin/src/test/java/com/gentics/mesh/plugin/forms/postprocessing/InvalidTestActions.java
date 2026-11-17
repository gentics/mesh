package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.Map;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

import io.reactivex.Completable;

/**
 * Container for annotated classes, which must be ignored by the {@link PostProcessingRegistry}
 */
public class InvalidTestActions {
	/**
	 * Annotated class not implementing {@link PostProcessingAction}
	 */
	@PostProcessing("not-an-action")
	public static class NotAnAction {
	}

	/**
	 * Action without public no-arg constructor
	 */
	@PostProcessing("no-default-constructor")
	public static class NoDefaultConstructor implements PostProcessingAction {
		public NoDefaultConstructor(String unused) {
		}

		@Override
		public boolean shouldProcess(Map<String, Object> properties, Form form, PostedData postedData, String language) {
			return true;
		}

		@Override
		public Completable process(Map<String, Object> properties, Form form, PostedData postedData, String language) {
			return Completable.complete();
		}
	}

}
