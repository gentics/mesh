package com.gentics.mesh.plugin.forms.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry that maps JSON type name strings to their {@link FormElement} implementation classes.
 *
 * <p>The registry is populated once at class-load time by scanning the
 * {@code com.gentics.mesh.plugin.forms.model} package for classes annotated with
 * {@link FormElementType}. No manual registration is required — adding a new element class
 * with the annotation is sufficient.
 */
public class ElementTypeRegistry {

	private static final Logger log = LoggerFactory.getLogger(ElementTypeRegistry.class);

	private static final Map<String, Class<? extends FormElement>> TYPE_MAP;

	static {
		Map<String, Class<? extends FormElement>> map = new HashMap<>();
		Reflections reflections = new Reflections("com.gentics.mesh.plugin.forms.model");
		Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(FormElementType.class);
		for (Class<?> clazz : annotated) {
			if (!FormElement.class.isAssignableFrom(clazz)) {
				log.warn("Class {} is annotated with @FormElementType but does not extend FormElement — skipping", clazz.getName());
				continue;
			}
			@SuppressWarnings("unchecked")
			Class<? extends FormElement> elementClass = (Class<? extends FormElement>) clazz;
			FormElementType annotation = clazz.getAnnotation(FormElementType.class);
			for (String typeName : annotation.value()) {
				Class<? extends FormElement> existing = map.put(typeName, elementClass);
				if (existing != null) {
					log.warn("Type name '{}' is claimed by both {} and {} — {} wins", typeName, existing.getSimpleName(), elementClass.getSimpleName(), elementClass.getSimpleName());
				}
			}
		}
		TYPE_MAP = Collections.unmodifiableMap(map);
		if (log.isDebugEnabled()) {
			log.debug("ElementTypeRegistry initialized with {} type mappings: {}", TYPE_MAP.size(), TYPE_MAP.keySet());
		}
	}

	private ElementTypeRegistry() {
	}

	/**
	 * Returns the implementation class for the given JSON type name, or {@code null} if unknown.
	 *
	 * @param type JSON type name (e.g. {@code "checkbox"}, {@code "string"})
	 * @return implementation class, or {@code null}
	 */
	public static Class<? extends FormElement> get(String type) {
		return TYPE_MAP.get(type);
	}
}
