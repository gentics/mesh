package com.gentics.cailun.etc;

@FunctionalInterface
public interface CaiLunCustomLoader<T> {

	void apply(T t) throws Exception;

}
