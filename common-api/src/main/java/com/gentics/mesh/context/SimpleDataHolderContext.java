package com.gentics.mesh.context;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.handler.DataHolderContext;

/**
 * Simple implementation of a {@link DataHolderContext}
 */
public class SimpleDataHolderContext implements DataHolderContext {
	/**
	 * Internal map to hold the data
	 */
	protected Map<String, Object> data = new HashMap<>();

	@Override
	public Map<String, Object> data() {
		return data;
	}

	@Override
	public DataHolderContext put(String key, Object obj) {
		data.put(key, obj);
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T get(String key) {
		return (T)data.get(key);
	}
}
