package com.gentics.mesh.hibernate.util;

import java.lang.reflect.Proxy;

/**
 * Dummy proxy factory for any class.
 * 
 * @author plyhun
 *
 */
public final class NoopProxy {
	private NoopProxy() {
	}

	/**
	 * Creates a proxy for any class.
	 * This proxy will do nothing for every method called and only return default values
	 * @param clazz
	 * @param <T>
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T create(Class<T> clazz) {
		return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[]{ clazz }, (proxy, method, args) -> {
			Class<?> returnType = method.getReturnType();
			if (returnType.isPrimitive()) {
				if (returnType.equals(boolean.class)) {
					return false;
				} else if (returnType.equals(void.class)) {
					return null;
				} else if ( returnType.equals(byte.class)) {
					return (byte)0;
				} else if ( returnType.equals(short.class)) {
					return (short)0;
				} else if ( returnType.equals(int.class)) {
					return 0;
				} else if ( returnType.equals(long.class)) {
					return 0l;
				} else if ( returnType.equals(float.class)) {
					return 0f;
				} else if ( returnType.equals(double.class)) {
					return 0d;
				} else if ( returnType.equals(char.class)) {
					return '\u0000';
				}
			}
			return null;
		});
	}
}
