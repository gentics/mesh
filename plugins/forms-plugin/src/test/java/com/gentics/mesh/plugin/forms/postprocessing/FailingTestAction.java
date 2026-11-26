package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.Strings;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

import io.reactivex.Completable;

/**
 * Test action, which is performed for form data where the field "input1" contains {@link #TRIGGER}, and always fails.
 */
@PostProcessing(FailingTestAction.KEY)
public class FailingTestAction implements PostProcessingAction {
	public static final String KEY = "failing";

	public static final String TRIGGER = "pp-fail";

	public static final String ERROR_MESSAGE = "Intentional failure";

	/**
	 * Number of calls to {@link #process(Map, Form, PostedData, String)}
	 */
	public static final AtomicInteger CALLS = new AtomicInteger();

	@Override
	public boolean shouldProcess(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		return Strings.CS.contains(postedData.getString("input1"), TRIGGER);
	}

	@Override
	public Completable process(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		CALLS.incrementAndGet();
		return Completable.error(new Exception(ERROR_MESSAGE));
	}
}
