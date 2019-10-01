package com.gentics.mesh.plugin.favorite.handler;

import com.gentics.mesh.plugin.PluginContext;

import io.reactivex.Completable;

/**
 * Abstract handler
 */
public abstract class AbstractFavoriteHandler {
	public abstract Completable handle(PluginContext context);


}
