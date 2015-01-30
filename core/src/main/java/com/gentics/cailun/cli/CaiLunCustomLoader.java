package com.gentics.cailun.cli;

@FunctionalInterface
public interface CaiLunCustomLoader<T> {

	void apply(T t) throws Exception;

}
