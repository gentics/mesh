package com.gentics.mesh.plugin.formgenerator.handler;

import com.gentics.mesh.plugin.PluginContext;

import io.reactivex.Completable;

/**
 * Abstract handler
 */
public abstract class AbstractFormGeneratorHandler {
	public abstract Completable handle(PluginContext context);
}
