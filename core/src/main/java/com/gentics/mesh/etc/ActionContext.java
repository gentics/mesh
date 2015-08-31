package com.gentics.mesh.etc;

public interface ActionContext {

	ActionContext put(String key, Object obj);

	<T> T get(String key);

}
