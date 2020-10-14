package com.gentics.mesh.plugin.forms.handler;

import com.gentics.mesh.plugin.PluginContext;

import io.reactivex.Completable;

/**
 * Abstract handler
 */
public abstract class AbstractFormsHandler {
	public abstract Completable handle(PluginContext context);
}
