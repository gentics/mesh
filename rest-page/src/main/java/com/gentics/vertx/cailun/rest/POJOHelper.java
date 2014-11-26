package com.gentics.vertx.cailun.rest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.neo4j.graphdb.Node;

public class POJOHelper {
	public static <T> T toPOJO(Class<T> clazz, Node n) throws InstantiationException, IllegalAccessException {
		T o = clazz.newInstance();

		for (String key : n.getPropertyKeys()) {
			Object value = n.getProperty(key);
			if (value != null) {
				setProperty(o, key, value);
			}
		}

		setProperty(o, "id", n.getId());

		return o;
	}

	public static void setProperty(Object o, String propertyName, Object value) {
		Class<? extends Object> clazz = o.getClass();
		try {
			Method setter = clazz.getMethod("set" + propertyName.substring(0, 1).toUpperCase() + propertyName.substring(1), value.getClass());
			setter.invoke(o, value);
		} catch (NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
		}
	}

	public static void toNode(Object o, Node n) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<? extends Object> clazz = o.getClass();
		Method[] methods = clazz.getMethods();
		for (Method method : methods) {
			if (method.getName().startsWith("getClass")) {
				continue;
			}
			if (method.getName().startsWith("get") && method.getTypeParameters().length == 0) {
				String propName = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
				Object value = method.invoke(o);
				if (value != null) {
					n.setProperty(propName, method.invoke(o));
				}
			}
		}
	}
}
