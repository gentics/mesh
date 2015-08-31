package com.gentics.mesh.etc.impl;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.etc.ActionContext;

public class ActionContextImpl implements ActionContext {

	private Map<String, Object> data;

	private Map<String, Object> getData() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		Object obj = getData().get(key);
		return (T) obj;
	}

	@Override
	public ActionContext put(String key, Object obj) {
		getData().put(key, obj);
		return this;
	}

}
