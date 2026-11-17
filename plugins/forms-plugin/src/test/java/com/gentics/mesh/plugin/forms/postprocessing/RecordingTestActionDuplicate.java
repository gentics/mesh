package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.Map;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

import io.reactivex.Completable;

/**
 * Action reusing the key of {@link RecordingTestAction}. Its class name sorts after {@link RecordingTestAction}, so it must be skipped by the {@link PostProcessingRegistry}.
 */
@PostProcessing(RecordingTestAction.KEY)
public class RecordingTestActionDuplicate implements PostProcessingAction {
	@Override
	public boolean shouldProcess(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		return true;
	}

	@Override
	public Completable process(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		return Completable.complete();
	}
}
