package com.gentics.mesh.search.plugins.permissions;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import java.util.Map;

/**
 * Created by philippguertler on 05.09.16.
 */
public class PermissionsPluginFactory implements NativeScriptFactory {
	@Override
	public ExecutableScript newScript(@Nullable Map<String, Object> map) {
		return new PermissionsScript();
	}

	@Override
	public boolean needsScores() {
		return false;
	}
}
