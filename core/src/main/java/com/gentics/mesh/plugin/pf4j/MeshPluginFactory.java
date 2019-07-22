package com.gentics.mesh.plugin.pf4j;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.pf4j.Plugin;
import org.pf4j.PluginFactory;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.plugin.MeshPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;

@Singleton
public class MeshPluginFactory implements PluginFactory {

	private static final Logger log = LoggerFactory.getLogger(MeshPluginFactory.class);

	private final PluginEnvironment pluginEnv;

	@Inject
	public MeshPluginFactory(PluginEnvironment pluginEnv) {
		this.pluginEnv = pluginEnv;
	}

	/**
	 * Creates a plugin instance. If an error occurs than that error is logged and the method returns null.
	 * 
	 * @param pluginWrapper
	 * @return
	 */
	@Override
	public Plugin create(final PluginWrapper pluginWrapper) {
		String pluginClassName = pluginWrapper.getDescriptor().getPluginClass();
		log.debug("Create instance for plugin '{}'", pluginClassName);

		Class<?> pluginClass;
		try {
			pluginClass = pluginWrapper.getPluginClassLoader().loadClass(pluginClassName);
		} catch (ClassNotFoundException e) {
			log.error(e.getMessage(), e);
			return null;
		}

		// once we have the class, we can do some checks on it to ensure
		// that it is a valid implementation of a plugin.
		int modifiers = pluginClass.getModifiers();
		if (Modifier.isAbstract(modifiers) || Modifier.isInterface(modifiers)
			|| (!MeshPlugin.class.isAssignableFrom(pluginClass))) {
			log.error("The plugin class '{}' is not a valid mesh plugin.", pluginClassName);
			return null;
		}

		// T t = constructor.newInstance(new Object[] { plugin });

		// create the plugin instance
		try {
			Constructor<?> constructor = pluginClass.getConstructor(new Class[] { PluginWrapper.class, PluginEnvironment.class });
			return (Plugin) constructor.newInstance(pluginWrapper, pluginEnv);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

}
