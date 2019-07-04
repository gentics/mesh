package com.gentics.mesh.core.data.search.request;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

import io.vertx.codegen.annotations.Fluent;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * A proxy for a {@link io.vertx.core.json.JsonObject} which caches the result of the {@link JsonObject#encode()} method.
 */
public class CachedJsonObjectProxy {
	private final JsonObject proxyTarget;
	private String encoded;

	public CachedJsonObjectProxy(JsonObject proxyTarget) {
		this.proxyTarget = proxyTarget;
	}

	public JsonObject getProxyTarget() {
		return proxyTarget;
	}

	public <T> T mapTo(Class<T> type) {
		return proxyTarget.mapTo(type);
	}

	public String getString(String key) {
		return proxyTarget.getString(key);
	}

	public Integer getInteger(String key) {
		return proxyTarget.getInteger(key);
	}

	public Long getLong(String key) {
		return proxyTarget.getLong(key);
	}

	public Double getDouble(String key) {
		return proxyTarget.getDouble(key);
	}

	public Float getFloat(String key) {
		return proxyTarget.getFloat(key);
	}

	public Boolean getBoolean(String key) {
		return proxyTarget.getBoolean(key);
	}

	public JsonObject getJsonObject(String key) {
		return proxyTarget.getJsonObject(key);
	}

	public JsonArray getJsonArray(String key) {
		return proxyTarget.getJsonArray(key);
	}

	public byte[] getBinary(String key) {
		return proxyTarget.getBinary(key);
	}

	public Instant getInstant(String key) {
		return proxyTarget.getInstant(key);
	}

	public Object getValue(String key) {
		return proxyTarget.getValue(key);
	}

	public String getString(String key, String def) {
		return proxyTarget.getString(key, def);
	}

	public Integer getInteger(String key, Integer def) {
		return proxyTarget.getInteger(key, def);
	}

	public Long getLong(String key, Long def) {
		return proxyTarget.getLong(key, def);
	}

	public Double getDouble(String key, Double def) {
		return proxyTarget.getDouble(key, def);
	}

	public Float getFloat(String key, Float def) {
		return proxyTarget.getFloat(key, def);
	}

	public Boolean getBoolean(String key, Boolean def) {
		return proxyTarget.getBoolean(key, def);
	}

	public JsonObject getJsonObject(String key, JsonObject def) {
		return proxyTarget.getJsonObject(key, def);
	}

	public JsonArray getJsonArray(String key, JsonArray def) {
		return proxyTarget.getJsonArray(key, def);
	}

	public byte[] getBinary(String key, byte[] def) {
		return proxyTarget.getBinary(key, def);
	}

	public Instant getInstant(String key, Instant def) {
		return proxyTarget.getInstant(key, def);
	}

	public Object getValue(String key, Object def) {
		return proxyTarget.getValue(key, def);
	}

	public boolean containsKey(String key) {
		return proxyTarget.containsKey(key);
	}

	public Set<String> fieldNames() {
		return proxyTarget.fieldNames();
	}

	public JsonObject put(String key, Enum value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, CharSequence value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, String value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Integer value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Long value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Double value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Float value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Boolean value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject putNull(String key) {
		return proxyTarget.putNull(key);
	}

	public JsonObject put(String key, JsonObject value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, JsonArray value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, byte[] value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Instant value) {
		return proxyTarget.put(key, value);
	}

	public JsonObject put(String key, Object value) {
		return proxyTarget.put(key, value);
	}

	public Object remove(String key) {
		return proxyTarget.remove(key);
	}

	public JsonObject mergeIn(JsonObject other) {
		return proxyTarget.mergeIn(other);
	}

	public JsonObject mergeIn(JsonObject other, boolean deep) {
		return proxyTarget.mergeIn(other, deep);
	}

	public JsonObject mergeIn(JsonObject other, int depth) {
		return proxyTarget.mergeIn(other, depth);
	}

	public String encode() {
		if (encoded == null) {
			encoded = proxyTarget.encode();
		}
		return encoded;
	}

	public String encodePrettily() {
		return proxyTarget.encodePrettily();
	}

	public Buffer toBuffer() {
		return proxyTarget.toBuffer();
	}

	public JsonObject copy() {
		return proxyTarget.copy();
	}

	public Map<String, Object> getMap() {
		return proxyTarget.getMap();
	}

	public Stream<Map.Entry<String, Object>> stream() {
		return proxyTarget.stream();
	}

	public Iterator<Map.Entry<String, Object>> iterator() {
		return proxyTarget.iterator();
	}

	public int size() {
		return proxyTarget.size();
	}

	@Fluent
	public JsonObject clear() {
		return proxyTarget.clear();
	}

	public boolean isEmpty() {
		return proxyTarget.isEmpty();
	}

	@Override
	public String toString() {
		return proxyTarget.toString();
	}

	@Override
	public boolean equals(Object o) {
		return proxyTarget.equals(o);
	}

	@Override
	public int hashCode() {
		return proxyTarget.hashCode();
	}

	public void writeToBuffer(Buffer buffer) {
		proxyTarget.writeToBuffer(buffer);
	}

	public int readFromBuffer(int pos, Buffer buffer) {
		return proxyTarget.readFromBuffer(pos, buffer);
	}

	public void forEach(Consumer<? super Map.Entry<String, Object>> action) {
		proxyTarget.forEach(action);
	}

	public Spliterator<Map.Entry<String, Object>> spliterator() {
		return proxyTarget.spliterator();
	}
}
