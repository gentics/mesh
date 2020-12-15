package com.gentics.mesh.etc;

/**
 * Custom loader to be used for adding custom verticles during startup.
 * 
 * @param <T>
 */
@FunctionalInterface
public interface MeshCustomLoader<T> {

	void apply(T t) throws Exception;

}
