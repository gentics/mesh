package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Jackson deserializer that detects the format of a {@code formGridOptions.defaultValue}
 * JSON node and produces the corresponding {@link DefaultValue} variant.
 *
 * <p>Detection rules (in order):</p>
 * <ol>
 *   <li>null node → {@code null}</li>
 *   <li>array node → {@link DefaultValue#list(List)}</li>
 *   <li>object node with {@code "userReference"} key → {@link DefaultValue#userReference(String)}</li>
 *   <li>object node (no {@code "userReference"}) → {@link DefaultValue#translation(Map)}</li>
 *   <li>scalar node (text / number / boolean) → {@link DefaultValue#scalar(Object)}</li>
 * </ol>
 *
 * <p>This extends Jackson's {@link StdDeserializer} standard base for concrete
 * deserializers, which still makes it a normal Jackson deserializer via
 * {@code JsonDeserializer}; the fixed target type is supplied through
 * {@code super(DefaultValue.class)}.</p>
 */
public class DefaultValueDeserializer extends StdDeserializer<DefaultValue> {

	public DefaultValueDeserializer() {
		super(DefaultValue.class);
	}

	@Override
	public DefaultValue deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
		JsonNode node = p.getCodec().readTree(p);
		if (node.isNull()) {
			return null;
		}
		if (node.isArray()) {
			List<Object> items = new ArrayList<>();
			node.forEach(n -> items.add(toScalar(n)));
			return DefaultValue.list(items);
		}
		if (node.isObject()) {
			if (node.has("userReference")) {
				return DefaultValue.userReference(node.get("userReference").asText());
			}
			Map<String, Object> map = new LinkedHashMap<>();
			node.fields().forEachRemaining(e -> map.put(e.getKey(), toScalar(e.getValue())));
			return DefaultValue.translation(map);
		}
		return DefaultValue.scalar(toScalar(node));
	}

	private Object toScalar(JsonNode n) {
		if (n.isTextual()) return n.asText();
		if (n.isNumber()) return n.numberValue();
		if (n.isBoolean()) return n.booleanValue();
		return n.asText();
	}
}
