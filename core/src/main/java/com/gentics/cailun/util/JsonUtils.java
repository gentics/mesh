package com.gentics.cailun.util;

import io.vertx.ext.apex.core.RoutingContext;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtils {

	protected static ObjectMapper mapper = new ObjectMapper();

	public static <T> String toJson(T obj) {
		try {
			return mapper.writeValueAsString(obj);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "ERROR";
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(RoutingContext rc, Class<?> classOfT) {
		try {
			String body = rc.getBodyAsString();
			return (T) mapper.readValue(body, classOfT);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
		// TODO compare with jackson
		// return (T) GSON.fromJson(rc.getBodyAsString(), classOfT);

	}
}
