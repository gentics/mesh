package com.gentics.mesh.api;

import com.gentics.mesh.api.impl.ActionContextImpl;

public interface ActionContext {

	ActionContext put(String key, Object obj);

	<T> T get(String key);

	/**
	 * Create a action context
	 *
	 * @return the body handler
	 */
	static ActionContext create() {
		return new ActionContextImpl();
	}
}
