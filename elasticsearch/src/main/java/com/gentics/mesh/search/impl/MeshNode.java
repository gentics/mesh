package com.gentics.mesh.search.impl;

import java.util.Collection;

import org.elasticsearch.Version;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.internal.InternalSettingsPreparer;
import org.elasticsearch.plugins.Plugin;

public class MeshNode extends Node {

	protected MeshNode(Settings settings, Collection<Class<? extends Plugin>> plugins) {
		super(InternalSettingsPreparer.prepareEnvironment(settings, null), Version.CURRENT, plugins);
	}
}
