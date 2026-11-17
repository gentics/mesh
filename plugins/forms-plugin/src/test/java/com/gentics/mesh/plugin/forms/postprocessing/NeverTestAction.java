package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.gentics.mesh.plugin.forms.model.Form;
import com.gentics.mesh.plugin.forms.model.schema.PostedData;

import io.reactivex.Completable;

/**
 * Test action, which never needs to be performed
 */
@PostProcessing(NeverTestAction.KEY)
public class NeverTestAction implements PostProcessingAction {
	public static final String KEY = "never";

	/**
	 * Number of calls to {@link #process(Map, Form, PostedData, String)}
	 */
	public static final AtomicInteger CALLS = new AtomicInteger();

	@Override
	public boolean shouldProcess(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		return false;
	}

	@Override
	public Completable process(Map<String, Object> properties, Form form, PostedData postedData, String language) {
		CALLS.incrementAndGet();
		return Completable.complete();
	}
}
