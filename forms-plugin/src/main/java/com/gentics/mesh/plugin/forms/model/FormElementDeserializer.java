package com.gentics.mesh.plugin.forms.model;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.plugin.forms.config.FormsConfig;

/**
 * Deserializer implementation for form elements
 *
 * @param <T>
 */
public class FormElementDeserializer<T extends FormElement> extends JsonDeserializer<T> {
	private final FormsConfig configuration;

	public FormElementDeserializer(FormsConfig configuration) {
		this.configuration = configuration;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		ObjectCodec oc = p.getCodec();
		JsonNode node = oc.readTree(p);
		ObjectMapper mapper = (ObjectMapper) p.getCodec();
		if (node.get("type") != null) {
			Class<? extends FormElement> clazz = null;
			String type = node.get("type").textValue();

			if (configuration.getElements() != null && configuration.getElements().containsKey(type)) {
				String className = configuration.getElements().get(type);
				try {
					Class<?> customClazz = Class.forName(className);
					if (FormElement.class.isAssignableFrom(customClazz)) {
						clazz = (Class<T>) customClazz;
					} else {
						throw new IOException(String.format("Cannot deserialize element of type %s, mapped class %s does not extend %s", type, className,
								FormElement.class.getName()));
					}
				} catch (ClassNotFoundException e) {
					throw new IOException(String.format("Cannot deserialize element of type %s into mapped class %s", type, className));
				}
			} else {
				ElementType elementType = ElementType.get(type);
				if (elementType != null) {
					clazz = elementType.getImplementationClass();
				} else {
					clazz = UnknownElement.class;
				}
			}

			T element = (T) mapper.convertValue(node, clazz);
			element.setConfiguration(configuration);
			return element;
		} else {
			JsonNode nameField = node.get("name");
			String fieldName = "unknown";
			if (nameField != null) {
				fieldName = nameField.textValue();
			}
			throw new JsonMappingException(p, "Missing type property for field {" + fieldName + "}", p.getCurrentLocation());
		}
	}
}
