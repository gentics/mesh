package com.gentics.mesh.util;

import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.response.SchemaFieldDeserializer;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.error.HttpStatusCodeErrorException;

@Component
public final class JsonUtils {

	@Autowired
	private I18NService i18nService;

	protected static ObjectMapper mapper;

	static {
		mapper = new ObjectMapper();
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		SimpleModule module = new SimpleModule();
		module.addDeserializer(FieldSchema.class, new SchemaFieldDeserializer());
		mapper.registerModule(module);

	}

	public static ObjectMapper getMapper() {
		return mapper;
	}

	public static <T> String toJson(T obj) throws HttpStatusCodeErrorException {
		try {
			return mapper.writeValueAsString(obj);
		} catch (IOException e) {
			// TODO i18n
			String message = "Could not generate json from object";
			// TODO 500?
			throw new HttpStatusCodeErrorException(500, message, e);
		}
	}

	public static <T> T readValue(String content, Class<T> valueType) throws IOException, JsonParseException, JsonMappingException {
		return mapper.readValue(content, valueType);
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromJson(RoutingContext rc, Class<?> classOfT) throws HttpStatusCodeErrorException {
		try {
			String body = rc.getBodyAsString();
			return (T) mapper.readValue(body, classOfT);
		} catch (Exception e) {
			throw new HttpStatusCodeErrorException(400, new I18NService().get(rc, "error_parse_request_json_error"), e);
		}

	}
}
