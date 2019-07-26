package com.gentics.mesh.plugin.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.pf4j.Extension;
import org.pf4j.ExtensionDescriptor;
import org.pf4j.ExtensionWrapper;
import org.pf4j.LegacyExtensionFinder;
import org.pf4j.PluginDependency;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MeshExtensionFinder extends LegacyExtensionFinder {

	private static final Logger log = LoggerFactory.getLogger(MeshExtensionFinder.class);

	public MeshExtensionFinder(PluginManager pluginManager) {
		super(pluginManager);
	}

	@Override
	public <T> List<ExtensionWrapper<T>> find(Class<T> type, String pluginId) {
		try {
			return super.find(type, pluginId);
		} catch (Throwable e) {
			log.warn("Failed to load the extension via default finder. Trying custom finder now.", e);
			return findByDependendClassloader(type, pluginId);
		}
	}

	private <T> List<ExtensionWrapper<T>> findByDependendClassloader(Class<T> type, String pluginId) {
		List<ExtensionWrapper<T>> result = new ArrayList<>();
		PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
		for (PluginDependency dep : pluginWrapper.getDescriptor().getDependencies()) {
			String depId = dep.getPluginId();
			PluginWrapper pw = pluginManager.getPlugin(depId);
			ClassLoader classLoader = pw.getPluginClassLoader();
			Set<String> classNames = findClassNames(pluginId);
			for (String className : classNames) {

				log.debug("Loading class '{}' using class loader '{}'", className, classLoader);
				try {
					Class<?> extensionClass = classLoader.loadClass(className);

					log.debug("Checking extension type '{}'", className);
					if (type.isAssignableFrom(extensionClass)) {
						ExtensionWrapper extensionWrapper = createExtensionWrapper(extensionClass);
						result.add(extensionWrapper);
						log.debug("Added extension '{}' with ordinal {}", className, extensionWrapper.getOrdinal());
					}
				} catch (ClassNotFoundException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		return result;
	}

	private ExtensionWrapper createExtensionWrapper(Class<?> extensionClass) {
		int ordinal = 0;
		if (extensionClass.isAnnotationPresent(Extension.class)) {
			ordinal = extensionClass.getAnnotation(Extension.class).ordinal();
		}
		ExtensionDescriptor descriptor = new ExtensionDescriptor(ordinal, extensionClass);

		return new ExtensionWrapper<>(descriptor, pluginManager.getExtensionFactory());
	}
}
