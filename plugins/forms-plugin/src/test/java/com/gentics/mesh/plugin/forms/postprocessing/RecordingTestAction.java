package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.Strings;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

import io.reactivex.Completable;

/**
 * Test action, which is performed for form data where the field "input1" contains {@link #TRIGGER}.
 * All invocations of {@link #process(Map, Form, PostedData, String)} are recorded.
 */
@PostProcessing(RecordingTestAction.KEY)
public class RecordingTestAction implements PostProcessingAction {
	public static final String KEY = "recording";

	public static final String TRIGGER = "pp-record";

	/**
	 * Recorded invocations
	 */
	public static final List<Invocation> INVOCATIONS = new CopyOnWriteArrayList<>();

	@Override
	public boolean shouldProcess(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		return Strings.CS.contains(postedData.getString("input1"), TRIGGER);
	}

	@Override
	public Completable process(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		return Completable.fromRunnable(() -> INVOCATIONS.add(new Invocation(properties, form.getUuid(), postedData.getString("input1"), language)));
	}

	/**
	 * Recorded invocation
	 */
	public static record Invocation(Map<String, Object> properties, String formUuid, String value, String language) {
	}
}
