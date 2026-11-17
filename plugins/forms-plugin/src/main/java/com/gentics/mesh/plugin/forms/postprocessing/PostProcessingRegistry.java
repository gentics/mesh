package com.gentics.mesh.plugin.forms.postprocessing;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of the available {@link PostProcessingAction} implementations.
 *
 * <p>The registry is populated by scanning the package {@link #SCAN_PACKAGE} for classes annotated with {@link PostProcessing}.
 * The scan is done once, when the registry is created (which is done when the plugin is started).
 */
public class PostProcessingRegistry {
	/**
	 * Package, which is scanned for implementations
	 */
	public static final String SCAN_PACKAGE = "com.gentics.mesh.plugin.forms.postprocessing";

	private static final Logger log = LoggerFactory.getLogger(PostProcessingRegistry.class);

	private final Map<String, PostProcessingAction> actions;

	/**
	 * Create an instance and scan for available post processing actions
	 */
	public PostProcessingRegistry() {
		Map<String, PostProcessingAction> map = new LinkedHashMap<>();
		Reflections reflections = new Reflections(SCAN_PACKAGE);

		// sort by class name, so that the result is deterministic, when the same key is used more than once
		reflections.getTypesAnnotatedWith(PostProcessing.class).stream().sorted(Comparator.comparing(Class::getName)).forEach(clazz -> {
			String key = clazz.getAnnotation(PostProcessing.class).value();
			if (StringUtils.isBlank(key)) {
				log.warn("Class {} is annotated with @PostProcessing with an empty key - skipping", clazz.getName());
				return;
			}
			if (!PostProcessingAction.class.isAssignableFrom(clazz)) {
				log.warn("Class {} is annotated with @PostProcessing but does not implement {} - skipping", clazz.getName(),
						PostProcessingAction.class.getSimpleName());
				return;
			}
			if (map.containsKey(key)) {
				log.warn("Key '{}' of class {} is already used by {} - skipping", key, clazz.getName(), map.get(key).getClass().getName());
				return;
			}
			try {
				map.put(key, (PostProcessingAction) clazz.getConstructor().newInstance());
			} catch (ReflectiveOperationException | RuntimeException e) {
				log.warn(String.format("Could not instantiate post processing action %s - skipping", clazz.getName()), e);
			}
		});

		actions = Collections.unmodifiableMap(map);
		log.info("Found {} post processing actions: {}", actions.size(), actions.keySet());
	}

	/**
	 * Get all available actions as unmodifiable map of key to instance
	 * @return map of actions
	 */
	public Map<String, PostProcessingAction> getActions() {
		return actions;
	}

	/**
	 * Get the action with given key
	 * @param key key
	 * @return optional action
	 */
	public Optional<PostProcessingAction> get(String key) {
		return Optional.ofNullable(actions.get(key));
	}
}
