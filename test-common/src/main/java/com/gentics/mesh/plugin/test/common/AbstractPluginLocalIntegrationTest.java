package com.gentics.mesh.plugin.test.common;

import com.gentics.mesh.test.local.MeshLocalServer;

import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * Abstract class for plugin tests which use the {@link MeshLocalServer} class to setup a mesh context.
 */
public abstract class AbstractPluginLocalIntegrationTest {

	static {
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	// TODO move local plugin code inside this class and away from individual tests 

}
