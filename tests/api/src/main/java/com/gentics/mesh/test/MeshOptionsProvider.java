package com.gentics.mesh.test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.StreamUtil;

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
	 * Get a clone of the options, which will share the storage options
	 * @return options clone
	 */
	public MeshOptions getClone() throws Exception;

	/**
	 * Resolve the provider instance, currently - from the system properties.
	 * 
	 * @return
	 */
	public static MeshOptionsProvider getProvider() {
		return spawnProviderInstance(getOptionalConfig(ENV_OPTIONS_PROVIDER_CLASS, (String)null, Function.identity(), Object::toString), MeshOptionsProvider.class);
	}
	
	@SuppressWarnings("unchecked")
	static <T extends MeshOptionsProvider> T spawnProviderInstance(String className, Class<T> classOfT) {
		if (StringUtils.isBlank(className)) {			
			return StreamUtil.toStream(ServiceLoader.load(classOfT)).findAny().orElseThrow(() -> new NoMeshTestContextException(classOfT));
		} else {
			try {
				return (T) MeshOptionsProvider.class.getClassLoader().loadClass(className).getConstructor().newInstance();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
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

	/**
	 * Get an optional configuration from either the environment variable, or the system parameter.
	 * 
	 * @param <T>
	 * @param configName
	 * @param defaultValue
	 * @param parse
	 * @param stringify
	 * @return
	 */
	static <T> T getOptionalConfig(String configName, T defaultValue, Function<String, T> parse, Function<T, String> stringify) {
		return parse.apply(System.getenv()
				.entrySet().stream()
				.filter(e -> e.getKey().equals(configName))
				.map(e -> e.getValue())
				.filter(Objects::nonNull)
				.findAny()
				.or(() -> Optional.ofNullable(System.getProperty(configName, stringify.apply(defaultValue))))
				.orElseGet(() -> stringify.apply(defaultValue)));
	}
}
