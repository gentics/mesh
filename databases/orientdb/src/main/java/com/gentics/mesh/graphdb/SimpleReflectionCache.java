package com.gentics.mesh.graphdb;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;

import com.gentics.madl.annotations.GraphElement;

/**
 * Cache for classes which were annotated with {@link GraphElement}.
 */
public class SimpleReflectionCache extends Reflections {

	private final Map<String, Set<String>> hierarchy;
	private final Map<Method, Map<Class<Annotation>, Annotation>> annotationCache = new HashMap<>();

	/**
	 * Mapping for class FQN to actual class reference.
	 */
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

	/**
	 * Return the set of sub types for the given class.
	 * 
	 * @param type
	 * @return
	 */
	public Set<? extends String> getSubTypeNames(final Class<?> type) {
		Set<String> subtypes = this.hierarchy.get(type.getName());
		if (subtypes == null)
			subtypes = Collections.singleton(type.getName());
		return Collections.unmodifiableSet(subtypes);
	}

	/**
	 * Return the set of sub types for the given class.
	 * 
	 * @param typeName
	 * @return
	 */
	public Set<? extends String> getSubTypeNames(final String typeName) {
		Set<String> subtypes = this.hierarchy.get(typeName);
		if (subtypes == null)
			subtypes = Collections.singleton(typeName);
		return Collections.unmodifiableSet(subtypes);
	}

	/**
	 * Return the annotation from the given method.
	 * 
	 * @param <E>
	 *            Annotation type
	 * @param method
	 *            Reference to the method
	 * @param annotationType
	 *            Type of the anntation to be loaded
	 * @return
	 */
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

	/**
	 * Resolve the class by name using the cache.
	 * 
	 * @param className
	 * @return
	 */
	public Class<?> forName(final String className) {
		return this.classStringCache.get(className);
	}

}
