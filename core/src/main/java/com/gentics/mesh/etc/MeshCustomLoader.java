package com.gentics.mesh.etc;

@FunctionalInterface
public interface MeshCustomLoader<T> {

	void apply(T t) throws Exception;

}
