package com.gentics.mesh.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.reflections.Reflections;
import org.testcontainers.shaded.org.apache.commons.lang.StringUtils;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * A runtime provider of context-specific {@link MeshOptions}. 
 * The implementations of this interface must be used by setting <code>optionsProviderClass=full.implementation.Class</code> in the system properties.
 * 
 * @author plyhun
 *
 */
public interface MeshOptionsProvider {

	/**
	 * System property key for an {@link MeshOptionsProvider} context-dependent implementation.
	 */
	public static final String ENV_OPTIONS_PROVIDER_CLASS = "optionsProviderClass";

	/**
	 * Provide the options instance.
	 * 
	 * @return
	 */
	public MeshOptions getOptions();
	
	/**
	 * Resolve the provider instance, currently - from the system properties.
	 * 
	 * @return
	 */
	public static MeshOptionsProvider getProvider() {
		return spawnProviderInstance(System.getProperty(ENV_OPTIONS_PROVIDER_CLASS), MeshOptionsProvider.class);
	}
	
	//TODO move off to Utils
	@SuppressWarnings("unchecked")
	static <T extends MeshOptionsProvider> T spawnProviderInstance(String className, Class<T> classOfT) {
		Class<? extends T> classToInstantiate = null;
		if (StringUtils.isBlank(className)) {
			Reflections reflections = new Reflections("com.gentics.mesh");
			List<Class<? extends T>> classes = new ArrayList<>(reflections.getSubTypesOf(classOfT));

			// remove interfaces, abstract classes or classes, which are not eligible
			classes.removeIf(cls -> cls.isInterface() || Modifier.isAbstract(cls.getModifiers()) || !isEligible(cls));

			if (!classes.isEmpty()) {
				// sort classes by order
				classes.sort((c1, c2) -> Integer.compare(getOrder(c1), getOrder(c2)));
				classToInstantiate = classes.get(0);
			}
			if (classToInstantiate == null) {
				throw new NoMeshTestContextException(classOfT);
			}
		} else {
			try {
				classToInstantiate = (Class<? extends T>) MeshOptionsProvider.class.getClassLoader().loadClass(className);
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		try {
			return (T) classToInstantiate.getConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the order of the provider class. This either the order given by the {@link MeshProviderOrder} annotation, or {@link Integer#MAX_VALUE}
	 * @param <T> type
	 * @param classOfT provider class
	 * @return order
	 */
	static <T extends MeshOptionsProvider> int getOrder(Class<T> classOfT) {
		MeshProviderOrder order = null;
		Class<?> clazz = classOfT;
		while (clazz != null && order == null) {
			order = clazz.getAnnotation(MeshProviderOrder.class);
			clazz = clazz.getSuperclass();
		}
		if (order == null) {
			return Integer.MAX_VALUE;
		} else {
			return order.value();
		}
	}

	/**
	 * Check whether the provider class is eligible. It is eligible, if it does not have a static method named "isEligible", which returns a boolean,
	 * or if that method returns true
	 * @param <T> type
	 * @param classOfT provider class
	 * @return true, when the class is eligible
	 */
	static <T extends MeshOptionsProvider> boolean isEligible(Class<T> classOfT) {
		try {
			Method method = classOfT.getMethod("isEligible");
			if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() == Boolean.class) {
				return (boolean) method.invoke(null);
			} else {
				return true;
			}
		} catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return true;
		}
	}
}
