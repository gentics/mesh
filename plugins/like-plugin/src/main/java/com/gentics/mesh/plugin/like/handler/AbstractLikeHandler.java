package com.gentics.mesh.plugin.like.handler;

import com.gentics.mesh.plugin.PluginContext;

import io.reactivex.Completable;

/**
 * Abstract handler
 */
public abstract class AbstractLikeHandler {
	public abstract Completable handle(PluginContext context);
}
