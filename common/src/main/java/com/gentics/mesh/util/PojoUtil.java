package com.gentics.mesh.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;

public final class PojoUtil {
	private PojoUtil() {
	}

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

	private static Pattern prefixRegex = Pattern.compile("^(get|set|is)(.*)");
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

	private static String lowerCaseFirstChar(String string) {
		return string.substring(0, 1).toLowerCase() + string.substring(1);
	}

	public static <T> T wrapException(Callable<T> callable) {
		try {
			return callable.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static class Property<T, P> {
		private final String name;
		private final Method getter;
		private final Method setter;

		private Property(String name, Method getter, Method setter) {
			this.name = name;
			this.setter = setter;
			this.getter = getter;
		}

		public String getName() {
			return name;
		}

		public P get(T object) {
			return wrapException(() -> (P) getter.invoke(object));
		}

		public Property<T, P> set(T object, P value) {
			wrapException(() -> setter.invoke(object, value));
			return this;
		}
	}
}
