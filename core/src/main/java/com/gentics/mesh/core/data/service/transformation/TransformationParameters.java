package com.gentics.mesh.core.data.service.transformation;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.AbstractResponse;

public class TransformationParameters {

	// Storage for object references
	private Map<String, AbstractResponse> objectReferences = new HashMap<>();

	public Map<String, AbstractResponse> getObjectReferences() {
		return objectReferences;
	}

	public AbstractResponse getObject(String uuid) {
		return objectReferences.get(uuid);
	}

	public void addObject(String uuid, AbstractResponse object) {
		objectReferences.put(uuid, object);
	}


}
