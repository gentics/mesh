package com.gentics.mesh.plugin.common.service;

import io.reactivex.Completable;

import java.util.function.Function;

/**
 * Convenience type definition for project initialization actions.
 */
public interface InitializationAction extends Function<String, Completable> {
}
