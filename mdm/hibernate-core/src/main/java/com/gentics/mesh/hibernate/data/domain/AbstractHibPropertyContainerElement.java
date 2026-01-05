package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MappedSuperclass;

/**
 * An abstraction over the general purpose key-value storage, where the value
 * can be provided as/parsed from JSON string. Please don't use it, unless you
 * have no other choice, i.e. storing a lot of small entities of a various type
 * in the same table.
 * 
 * @author plyhun
 *
 */
@MappedSuperclass
public abstract class AbstractHibPropertyContainerElement extends AbstractHibBaseElement {

	private static final String VALUE = "_value";
	private static final String IS_JSON = "_is_json";
	private static final String CLASS_NAME = "_class_name";

	@ElementCollection
	@CollectionTable
	@MapKeyColumn(name = "pkey")
	@Column(name = "pvalue", length = MeshOptions.DEFAULT_STRING_LENGTH)
	private Map<String, String> properties = new HashMap<>();

	@SuppressWarnings("unchecked")
	public <R> R property(String name) {
		String value = properties.get(name);
		if (StringUtils.isBlank(value)) {
			return null;
		}
		JsonObject jsonObject = new JsonObject(value);

		String className = jsonObject.getString(CLASS_NAME);
		if (StringUtils.isBlank(className)) {
			return null;
		}
		Boolean isJson = jsonObject.getBoolean(IS_JSON, false);
		Object ovalue = jsonObject.getValue(VALUE);
		if (ovalue == null) {
			return null;
		}
		boolean isArray = ovalue instanceof JsonArray;
		try {
			Class<?> cls = getClass().getClassLoader().loadClass(className);
			if (isArray) {
				JsonArray ja = jsonObject.getJsonArray(VALUE);
				Object array = Array.newInstance(cls, ja.size());
				for (int i = 0; i < ja.size(); i++) {
					Object item = ja.getValue(i);
					item = isJson ? JsonUtil.readValue(item.toString(), cls) : item;
					Array.set(array, i, item);
				}
				return (R) array;
			} else if (isJson) {
				return (R) JsonUtil.readValue(ovalue.toString(), cls);
			} else {
				return (R) ovalue;
			}
		} catch (Throwable e) {
			throw error(INTERNAL_SERVER_ERROR, "Failed to get property value {" + name + "}", e);
		}
	}

	/**
	 * Set the property.
	 *
	 * @param key
	 * @param value
	 */
	public <R> void property(String key, R value) {
		JsonObject json = new JsonObject();
		if (value != null) {
			json.put(IS_JSON, value instanceof RestModel);
			if (value.getClass().isArray()) {
				JsonArray array = new JsonArray();
				for (int i = 0; i < Array.getLength(value); i++) {
					array.add(Array.get(value, i));
				}
				json.put(CLASS_NAME, value.getClass().getComponentType().getCanonicalName());
				json.put(VALUE, array);
			} else if (Collection.class.isInstance(value)) {
				JsonArray array = new JsonArray();
				json.put(CLASS_NAME, Object.class.getCanonicalName());
				for (Object o : Collection.class.cast(value)) {
					array.add(o);
					json.put(CLASS_NAME, o.getClass().getCanonicalName());
				}
				json.put(VALUE, array);
			} else {
				json.put(CLASS_NAME, value.getClass().getCanonicalName());
				json.put(VALUE, value);
			}
			properties.put(key, json.encode());
		} else {
			json.put(CLASS_NAME, StringUtils.EMPTY);
			properties.put(key, StringUtils.EMPTY);
		}
	}

	/**
	 * Remove the property with the given key.
	 *
	 * @param key
	 */
	public void removeProperty(String key) {
		properties.remove(key);
	}

	/**
	 * Return all stored properties in inner JSON format.
	 * 
	 * @return Found properties
	 */
	protected Map<String, String> getJsonProperties() {
		return properties;
	}

	/**
	 * Return all stored properties, with a try to parse each to the corresponding
	 * entity.
	 * 
	 * @param <R>
	 * @return
	 */
	public <R> Map<String, R> getRestProperties() {
		Map<String, R> map = new HashMap<>(properties.size());
		getJsonProperties().entrySet().stream().forEach(e -> map.put(e.getKey(), property(e.getKey())));
		return map;
	}
}
