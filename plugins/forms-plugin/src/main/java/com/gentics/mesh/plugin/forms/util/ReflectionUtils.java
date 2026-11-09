package com.gentics.mesh.plugin.forms.util;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReflectionUtils {

	private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

	private static Class<?> getTargetClass(String className, Class<?> expectedBase) {
		if (className == null || className.isEmpty()) {
			log.warn("No class name specified");
			return null;
		}
		if (expectedBase == null) {
			log.warn("No expected base class specified");
			return null;
		}
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException e) {
			log.warn("No class '{}' found", className);
		}
		return null;
	}

	/**
	 * Get an instance of the class with the given classname.
	 *
	 * This method makes sure the created object is assignable to the <code>expectedBase</code> class.
	 *
	 * If anything goes wrong the errors will be logged, and the return value will be null, but no exceptions will be thrown.
	 *
	 * @param className
	 *            The class to create an instance of
	 * @param expectedBase
	 *            The expected super class of the created instance
	 * @param desc
	 *            A description of the class used for error message
	 *
	 * @return An instance of the specified class
	 */
	@SuppressWarnings("unchecked")
	public static <E> E getInstance(String className, Class<E> expectedBase, String desc) {
		Class<?> targetClass = null;

		if (desc == null || desc.isEmpty()) {
			desc = "(no description)";
		}

		targetClass = getTargetClass(className, expectedBase);

		if (targetClass == null) {
			log.error("Invalid {} specified", desc);
			return null;
		}

		if (!expectedBase.isAssignableFrom(targetClass)) {
			log.error("Specified {} is not an implementation of {}", desc, expectedBase.getSimpleName());
			return null;
		}

		Constructor<E> ctor = null;

		try {
			ctor = ((Class<E>) targetClass).getConstructor();
		} catch (NoSuchMethodException e) {
			log.error("The " + desc + " class " + className + " does not have a default constructor", e);
		} catch (SecurityException e) {
			log.error("Could not instantiate class " + desc, e);
		}

		if (ctor == null) {
			log.error("No valid constructor found for {}", desc);
		} else {
			try {
				return ctor.newInstance();
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.error("Could not instantiate class " + desc, e);
			}
		}
		return null;
	}

	/**
	 * More lenient version of an enums <code>valueOf()</code> method.
	 *
	 * This method will search the values of the specified enum for one equal to <code>name</code>, but in contrast to <code>valueOf()</code> ignores the case.
	 *
	 * @param enumType
	 *            The class of the desired enum type
	 * @param name
	 *            The case independent name of an value of <code>enumType</code>
	 * @param defaultValue
	 *            The default value to be returned when no value for <code>name</code> exists.
	 *
	 * @return A value of the specified enum type with the specified name (ignoring case), or <code>defaultValue</code> if no such value exists.
	 */
	public static <E extends Enum<E>> E getEnumValue(Class<E> enumType, String name, E defaultValue) {
		if (name == null || name.isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No enum value name supplied, using default: {}", defaultValue.name());
			}
			return defaultValue;
		}

		E[] values;

		try {
			Method method = enumType.getMethod("values");
			@SuppressWarnings("unchecked")
			E[] invokeResult = (E[]) method.invoke(null);

			values = invokeResult;
		} catch (ClassCastException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException
			| InvocationTargetException e) {
			log.error("Could not get enum values of " + enumType.getName(), e);

			if (log.isDebugEnabled()) {
				log.debug("Using default: {}", defaultValue.name());
			}
			return defaultValue;
		}

		for (E value : values) {
			if (name.toLowerCase().equals(value.name().toLowerCase())) {
				if (log.isDebugEnabled()) {
					log.debug(
						"Matching value found in {} for \"{}\": {}",
						enumType.getName(),
						name,
						value.name());
				}

				return value;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug(
				"Invalid value \"{}\" for enum {}, using default: {}",
				name,
				enumType.getName(),
				defaultValue.name());
		}

		return defaultValue;
	}
}
