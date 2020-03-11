package com.gentics.madl;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections.Reflections;

import com.gentics.madl.annotations.GraphElement;

/**
 * Type cache which also provides resolving methods which cache the result.
 */
public class ElementTypeClassCache {

	private final Map<String, Class> classStringCache = new ConcurrentHashMap<>();
	private String[] basePaths;

	public ElementTypeClassCache(String... packagePaths) {
		this.basePaths = packagePaths;
	}

	public Class forName(final String className) {
		return this.classStringCache.computeIfAbsent(className, (key) -> {
			for (String basePath : basePaths) {
				Set<Class<?>> graphTypeClasses = new Reflections(basePath).getTypesAnnotatedWith(GraphElement.class);
				for (Class<?> clazz : graphTypeClasses) {
					if (clazz.getSimpleName().equals(key)) {
						return clazz;
					}
				}
			}
			throw new IllegalStateException("The class {" + className + "} cannot be found for basePaths {" + Arrays.toString(basePaths) + "}");
		});
	}
}
