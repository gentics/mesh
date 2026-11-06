package com.gentics.mesh.plugin.favorite.handler;

import com.gentics.mesh.plugin.PluginContext;

import com.gentics.mesh.plugin.favorite.FavoriteService;
import io.reactivex.Completable;

import java.util.regex.Pattern;

/**
 * Abstract handler
 */
public abstract class AbstractFavoriteHandler {
	public abstract Completable handle(PluginContext context);
	public final String regexMatch = "[A-Za-z0-9\\-\\_]+";
	public final Pattern pattern = Pattern.compile(regexMatch);

	public final Pattern idPattern = Pattern.compile(".*");

}
