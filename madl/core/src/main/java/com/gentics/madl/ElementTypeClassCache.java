package com.gentics.madl;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.reflections8.Reflections;

import com.gentics.madl.annotations.GraphElement;

/**
 * Type cache which also provides resolving methods which cache the result.
 */
public class ElementTypeClassCache<T> {

	private final Map<T, Class<?>> classStringCache = new ConcurrentHashMap<>();
	private String[] basePaths;

	public ElementTypeClassCache(String... packagePaths) {
		this.basePaths = packagePaths;
	}

	public Class<?> forId(final T classId) {
		return this.classStringCache.computeIfAbsent(classId, (key) -> {
			for (String basePath : basePaths) {
				Set<Class<?>> graphTypeClasses = new Reflections(basePath).getTypesAnnotatedWith(GraphElement.class);
				for (Class<?> clazz : graphTypeClasses) {
					if (clazz.getSimpleName().equals(key)) {
						return clazz;
					}
				}
			}
			throw new IllegalStateException("The class {" + classId + "} cannot be found for basePaths {" + Arrays.toString(basePaths) + "}");
		});
	}
}
