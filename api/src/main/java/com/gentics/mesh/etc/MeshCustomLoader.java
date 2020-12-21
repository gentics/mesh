package com.gentics.mesh.etc;

/**
 * Custom loader to be used for adding custom verticles during startup.
 * 
 * @param <T>
 */
@FunctionalInterface
public interface MeshCustomLoader<T> {

	/**
	 * Apply the load operation.
	 * 
	 * @param t
	 * @throws Exception
	 */
	void apply(T t) throws Exception;

}
