package com.gentics.mda;

@FunctionalInterface
public interface ATxAction<T> {

	T handle(ATx tx) throws Exception;

}
