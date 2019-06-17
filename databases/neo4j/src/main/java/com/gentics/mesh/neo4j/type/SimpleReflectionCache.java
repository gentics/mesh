package com.gentics.mesh.neo4j.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import com.gentics.madl.annotations.GraphElement;

public class SimpleReflectionCache extends Reflections {

	private final Map<String, Set<String>> hierarchy;
	private final Map<Method, Map<Class<Annotation>, Annotation>> annotationCache = new HashMap<>();
	private final Map<String, Class> classStringCache = new HashMap<>();

	public SimpleReflectionCache() {
		super();
		this.hierarchy = new HashMap<>();
	}

	public SimpleReflectionCache(String... basePaths) {
		this();
		for (String basePath : basePaths) {
			Set<Class<?>> graphTypeClasses = new Reflections(basePath).getTypesAnnotatedWith(GraphElement.class);
			for (Class<?> clazz : graphTypeClasses) {
				classStringCache.put(clazz.getSimpleName(), clazz);
			}
		}
	}

	public Set<? extends String> getSubTypeNames(final Class<?> type) {
		Set<String> subtypes = this.hierarchy.get(type.getName());
		if (subtypes == null)
			subtypes = Collections.singleton(type.getName());
		return Collections.unmodifiableSet(subtypes);
	}

	public Set<? extends String> getSubTypeNames(final String typeName) {
		Set<String> subtypes = this.hierarchy.get(typeName);
		if (subtypes == null)
			subtypes = Collections.singleton(typeName);
		return Collections.unmodifiableSet(subtypes);
	}

	public <E extends Annotation> E getAnnotation(final Method method, final Class<E> annotationType) {
		Map<Class<Annotation>, Annotation> annotationsPresent = annotationCache.get(method);
		if (annotationsPresent == null) {
			annotationsPresent = new HashMap<>();
			annotationCache.put(method, annotationsPresent);
		}

		E annotation = (E) annotationsPresent.get(annotationType);
		if (annotation == null) {
			annotation = method.getAnnotation(annotationType);
			annotationsPresent.put((Class<Annotation>) annotationType, annotation);
		}
		return annotation;
	}

	public Class<?> forName(final String className) {
		return this.classStringCache.get(className);
	}

}
