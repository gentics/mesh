package com.gentics.mesh.util;

import static com.gentics.mesh.util.StringUtil.lowerCaseFirstChar;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class PojoUtil {
	private PojoUtil() {
	}
	private static Pattern prefixRegex = Pattern.compile("^(get|set|is)(.*)");

	/**
	 * Assigns properties to the first pojo from the other pojos similar to JavaScripts <code>Object.assign()</code>.
	 * Getters returning null values are ignored.
	 *
	 * @param dest
	 * @param src
	 * @param <T>
	 * @return
	 */
	public static <T> T assignIgnoringNull(T dest, T... src) {
		List<Property> properties = getProperties(dest.getClass()).collect(Collectors.toList());
		for (T obj : src) {
			for (Property<T, Object> prop : properties) {
				Object value = prop.get(obj);
				if (value != null) {
					prop.set(dest, value);
				}
			}
		}

		return dest;
	}

	/**
	 * Gets all properties of a class. A property is a pair of methods (getter/setter) to access an instance variable.
	 *
	 * In addition the methods must fulfill the following conditions in order to be considered part of a property.
	 * <ul>
	 *     <li>Both methods must be public.</li>
	 *     <li>Both methods must not be annotated with {@link JsonIgnore}.</li>
	 *     <li>The name of the getter must start with <code>get</code> or <code>is</code>.</li>
	 *     <li>The name of the setter must start with <code>set</code></li>
	 *     <li>The part of the name after the prefix (get/is/set) must be the same for both methods.</li>
	 * </ul>
	 *
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	public static <T> Stream<Property<T, ?>> getProperties(Class<T> clazz) {
		Map<String, Method[]> properties = new HashMap<>();
		for (Method method : clazz.getMethods()) {
			if (method.getAnnotation(JsonIgnore.class) != null || !Modifier.isPublic(method.getModifiers())) {
				continue;
			}
			String methodName = method.getName();
			Matcher matcher = prefixRegex.matcher(methodName);
			if (!matcher.find()) {
				continue;
			}
			String prefix = matcher.group(1);
			String name = matcher.group(2);
			int methodIndex = prefix.equals("set") ? 1 : 0;
			Method[] methods = properties.computeIfAbsent(name, key -> new Method[2]);
			if (methods[methodIndex] != null) {
				throw new RuntimeException("Duplicate property method found: " + methodName);
			}
			methods[methodIndex] = method;
		}

		return properties.entrySet().stream()
			.filter(entry -> entry.getValue()[0] != null && entry.getValue()[1] != null)
			.map(entry -> {
				Method[] methods = entry.getValue();
				return new Property<>(lowerCaseFirstChar(entry.getKey()), methods[0], methods[1]);
			});
	}

	/**
	 * A property (getter/setter) of a POJO.
	 * @param <T> The type of the class in which the property is defined.
	 * @param <V> The type of the value that is accessed via this property.
	 */
	public static class Property<T, V> {
		private final String name;
		private final Method getter;
		private final Method setter;

		private Property(String name, Method getter, Method setter) {
			this.name = name;
			this.setter = setter;
			this.getter = getter;
		}

		/**
		 * Get the name of the property.
		 * @return
		 */
		public String getName() {
			return name;
		}

		/**
		 * Get the value of the property of a POJO.
		 * @param object
		 * @return
		 */
		public V get(T object) {
			return ExceptionUtil.wrapException(() -> (V) getter.invoke(object));
		}

		/**
		 * Set the value of the property of a POJO.
		 * @param object
		 * @param value
		 * @return
		 */
		public Property<T, V> set(T object, V value) {
			ExceptionUtil.wrapException(() -> setter.invoke(object, value));
			return this;
		}
	}
}
