package com.gentics.mesh.plugin.test.common.util;

import java.io.File;

@FunctionalInterface
public interface ConfigSetupAction {

	void setup(File configDir) throws Exception;
}
