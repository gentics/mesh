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

	/**
	 * @see JsonObject#mapTo(Class)
	 */
	public <T> T mapTo(Class<T> type) {
		return proxyTarget.mapTo(type);
	}

	/**
	 * @see JsonObject#getString(String)
	 */
	public String getString(String key) {
		return proxyTarget.getString(key);
	}

	/**
	 * @see JsonObject#getInteger(String)
	 */
	public Integer getInteger(String key) {
		return proxyTarget.getInteger(key);
	}

	/**
	 * @see JsonObject#getLong(String)
	 */
	public Long getLong(String key) {
		return proxyTarget.getLong(key);
	}

	/**
	 * @see JsonObject#getDouble(String)
	 */
	public Double getDouble(String key) {
		return proxyTarget.getDouble(key);
	}

	/**
	 * @see JsonObject#getFloat(String)
	 */
	public Float getFloat(String key) {
		return proxyTarget.getFloat(key);
	}

	/**
	 * @see JsonObject#getBoolean(String)
	 */
	public Boolean getBoolean(String key) {
		return proxyTarget.getBoolean(key);
	}

	/**
	 * @see JsonObject#getJsonObject(String)
	 */
	public JsonObject getJsonObject(String key) {
		return proxyTarget.getJsonObject(key);
	}

	/**
	 * @see JsonObject#getJsonArray(String)
	 */
	public JsonArray getJsonArray(String key) {
		return proxyTarget.getJsonArray(key);
	}

	/**
	 * @see JsonObject#getBinary(String)
	 */
	public byte[] getBinary(String key) {
		return proxyTarget.getBinary(key);
	}

	/**
	 * @see JsonObject#getInstant(String)
	 */
	public Instant getInstant(String key) {
		return proxyTarget.getInstant(key);
	}

	/**
	 * @see JsonObject#getValue(String)
	 */
	public Object getValue(String key) {
		return proxyTarget.getValue(key);
	}

	/**
	 * @see JsonObject#getString(String, String)
	 */
	public String getString(String key, String def) {
		return proxyTarget.getString(key, def);
	}

	/**
	 * @see JsonObject#getInteger(String, Integer)
	 */
	public Integer getInteger(String key, Integer def) {
		return proxyTarget.getInteger(key, def);
	}

	/**
	 * @see JsonObject#getLong(String, Long)
	 */
	public Long getLong(String key, Long def) {
		return proxyTarget.getLong(key, def);
	}

	/**
	 * @see JsonObject#getDouble(String, Double)
	 */
	public Double getDouble(String key, Double def) {
		return proxyTarget.getDouble(key, def);
	}

	/**
	 * @see JsonObject#getFloat(String, Float)
	 */
	public Float getFloat(String key, Float def) {
		return proxyTarget.getFloat(key, def);
	}

	/**
	 * @see JsonObject#getBoolean(String, Boolean)
	 */
	public Boolean getBoolean(String key, Boolean def) {
		return proxyTarget.getBoolean(key, def);
	}

	/**
	 * @see JsonObject#getJsonObject(String, JsonObject)
	 */
	public JsonObject getJsonObject(String key, JsonObject def) {
		return proxyTarget.getJsonObject(key, def);
	}

	/**
	 * @see JsonObject#getJsonArray(String, JsonArray)
	 */
	public JsonArray getJsonArray(String key, JsonArray def) {
		return proxyTarget.getJsonArray(key, def);
	}

	/**
	 * @see JsonObject#getBinary(String, byte[])
	 */
	public byte[] getBinary(String key, byte[] def) {
		return proxyTarget.getBinary(key, def);
	}

	/**
	 * @see JsonObject#getInstant(String, Instant)
	 */
	public Instant getInstant(String key, Instant def) {
		return proxyTarget.getInstant(key, def);
	}

	/**
	 * @see JsonObject#getValue(String, Object)
	 */
	public Object getValue(String key, Object def) {
		return proxyTarget.getValue(key, def);
	}

	/**
	 * @see JsonObject#containsKey(String)
	 */
	public boolean containsKey(String key) {
		return proxyTarget.containsKey(key);
	}

	/**
	 * @see JsonObject#fieldNames()
	 */
	public Set<String> fieldNames() {
		return proxyTarget.fieldNames();
	}

	/**
	 * @see JsonObject#
	 */
	public JsonObject put(String key, Enum value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, CharSequence)
	 */
	public JsonObject put(String key, CharSequence value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, String)
	 */
	public JsonObject put(String key, String value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Integer)
	 */
	public JsonObject put(String key, Integer value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Long)
	 */
	public JsonObject put(String key, Long value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Double)
	 */
	public JsonObject put(String key, Double value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Float)
	 */
	public JsonObject put(String key, Float value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Boolean)
	 */
	public JsonObject put(String key, Boolean value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#putNull(String)
	 */
	public JsonObject putNull(String key) {
		return proxyTarget.putNull(key);
	}

	/**
	 * @see JsonObject#put(String, JsonObject)
	 */
	public JsonObject put(String key, JsonObject value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, JsonArray)
	 */
	public JsonObject put(String key, JsonArray value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, byte[])
	 */
	public JsonObject put(String key, byte[] value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Instant)
	 */
	public JsonObject put(String key, Instant value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#put(String, Object)
	 */
	public JsonObject put(String key, Object value) {
		return proxyTarget.put(key, value);
	}

	/**
	 * @see JsonObject#remove(String)
	 */
	public Object remove(String key) {
		return proxyTarget.remove(key);
	}

	/**
	 * @see JsonObject#mergeIn(JsonObject)
	 */
	public JsonObject mergeIn(JsonObject other) {
		return proxyTarget.mergeIn(other);
	}

	/**
	 * @see JsonObject#mergeIn(JsonObject, boolean)
	 */
	public JsonObject mergeIn(JsonObject other, boolean deep) {
		return proxyTarget.mergeIn(other, deep);
	}

	/**
	 * @see JsonObject#mergeIn(JsonObject, int)
	 */
	public JsonObject mergeIn(JsonObject other, int depth) {
		return proxyTarget.mergeIn(other, depth);
	}

	/**
	 * @see JsonObject#encode()
	 */
	public String encode() {
		if (encoded == null) {
			encoded = proxyTarget.encode();
		}
		return encoded;
	}

	/**
	 * @see JsonObject#encodePrettily()
	 */
	public String encodePrettily() {
		return proxyTarget.encodePrettily();
	}

	/**
	 * @see JsonObject#toBuffer()
	 */
	public Buffer toBuffer() {
		return proxyTarget.toBuffer();
	}

	/**
	 * @see JsonObject#copy()
	 */
	public JsonObject copy() {
		return proxyTarget.copy();
	}

	/**
	 * @see JsonObject#getMap()
	 */
	public Map<String, Object> getMap() {
		return proxyTarget.getMap();
	}

	/**
	 * @see JsonObject#stream()
	 */
	public Stream<Map.Entry<String, Object>> stream() {
		return proxyTarget.stream();
	}

	/**
	 * @see JsonObject#iterator()
	 */
	public Iterator<Map.Entry<String, Object>> iterator() {
		return proxyTarget.iterator();
	}

	/**
	 * @see JsonObject#size()
	 */
	public int size() {
		return proxyTarget.size();
	}

	/**
	 * @see JsonObject#clear()
	 */
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

	/**
	 * @see JsonObject#writeToBuffer(Buffer)
	 */
	public void writeToBuffer(Buffer buffer) {
		proxyTarget.writeToBuffer(buffer);
	}

	/**
	 * @see JsonObject#readFromBuffer(int, Buffer)
	 */
	public int readFromBuffer(int pos, Buffer buffer) {
		return proxyTarget.readFromBuffer(pos, buffer);
	}

	/**
	 * @see JsonObject#forEach(Consumer)
	 */
	public void forEach(Consumer<? super Map.Entry<String, Object>> action) {
		proxyTarget.forEach(action);
	}

	/**
	 * @see JsonObject#spliterator()
	 */
	public Spliterator<Map.Entry<String, Object>> spliterator() {
		return proxyTarget.spliterator();
	}
}
