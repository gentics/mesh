package com.gentics.mesh.core.data.service.transformation;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class TransformationParameters {

	// Storage for object references
	private Map<String, AbstractRestModel> objectReferences = new HashMap<>();

	public Map<String, AbstractRestModel> getObjectReferences() {
		return objectReferences;
	}

	public AbstractRestModel getObject(String uuid) {
		return objectReferences.get(uuid);
	}

	public void addObject(String uuid, AbstractRestModel object) {
		objectReferences.put(uuid, object);
	}


}
