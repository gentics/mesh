package com.gentics.mesh.plugin.forms.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

/**
 * Typed representation of a {@code formGridOptions.defaultValue} entry.
 *
 * <p>The four supported formats and their {@link Type}:</p>
 * <ul>
 *   <li>{@code "text"} / {@code 42} → {@link Type#SCALAR}</li>
 *   <li>{@code ["a", "b"]} → {@link Type#LIST}</li>
 *   <li>{@code {"userReference": "email"}} → {@link Type#USER_REFERENCE}</li>
 *   <li>{@code {"de": "Text", "en": "Text"}} → {@link Type#TRANSLATION}</li>
 * </ul>
 *
 * <p>Use {@link #resolve(String, Map)} to obtain the effective value at runtime.
 * Jackson deserializes this class automatically via {@link DefaultValueDeserializer}.</p>
 */
@JsonDeserialize(using = DefaultValueDeserializer.class)
public final class DefaultValue {

	public enum Type { SCALAR, LIST, USER_REFERENCE, TRANSLATION }

	private final Type kind;
	private final Object raw;

	private DefaultValue(Type kind, Object raw) {
		this.kind = kind;
		this.raw = raw;
	}

	public static DefaultValue scalar(Object value) {
		return new DefaultValue(Type.SCALAR, value);
	}

	public static DefaultValue list(List<Object> items) {
		return new DefaultValue(Type.LIST, items);
	}

	public static DefaultValue userReference(String claimKey) {
		return new DefaultValue(Type.USER_REFERENCE, claimKey);
	}

	public static DefaultValue translation(Map<String, Object> translations) {
		return new DefaultValue(Type.TRANSLATION, translations);
	}

	public Type getKind() {
		return kind;
	}

	/** Returns {@code true} when this default carries a list of values ({@link Type#LIST}). */
	public boolean isList() {
		return kind == Type.LIST;
	}

	/**
	 * Resolves the effective value for the given language and JWT claims.
	 *
	 * <ul>
	 *   <li>SCALAR / LIST → raw value returned as-is</li>
	 *   <li>USER_REFERENCE → {@code jwtClaims.get(claimKey)}, or {@code null} when claims are absent</li>
	 *   <li>TRANSLATION → value for the given {@code language} key</li>
	 * </ul>
	 */
	public Object resolve(String language, Map<String, Object> jwtClaims) {
		switch (kind) {
			case SCALAR:
			case LIST:
				return raw;
			case USER_REFERENCE:
				return jwtClaims != null ? jwtClaims.get((String) raw) : null;
			case TRANSLATION:
				return ((Map<?, ?>) raw).get(language);
			default:
				return null;
		}
	}
}
