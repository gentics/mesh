package com.gentics.mesh.search.plugin;

import java.util.Map;

import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.script.ExecutableScript;
import org.elasticsearch.script.NativeScriptFactory;

import com.gentics.mesh.search.impl.MeshNode;

public class PermissionsPluginFactory implements NativeScriptFactory {

	private MeshNode meshNode;

	@Inject
	public PermissionsPluginFactory(Node node, Settings settings) {
		if (node instanceof MeshNode) {
			this.meshNode = (MeshNode) node;
		} else {
			throw new RuntimeException("The permission plugin only works in a Gentics Mesh server context");
		}
	}

	@Override
	public ExecutableScript newScript(@Nullable Map<String, Object> params) {
		return new PermissionsScript(params, meshNode);
	}

	@Override
	public boolean needsScores() {
		return false;
	}

}
