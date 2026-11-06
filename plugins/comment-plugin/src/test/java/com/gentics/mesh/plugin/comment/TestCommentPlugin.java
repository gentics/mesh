package com.gentics.mesh.plugin.comment;

import java.io.File;
import java.net.URISyntaxException;

import org.pf4j.PluginWrapper;

import com.gentics.mesh.plugin.env.PluginEnvironment;

public class TestCommentPlugin extends CommentPlugin {

	public static String config;

	public TestCommentPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	protected File getConfigFile() {
		try {
			return new File(TestCommentPlugin.class.getResource("/" + config + ".yml").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File getStorageDir() {
		try {
			return new File(TestCommentPlugin.class.getResource("/storage").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
