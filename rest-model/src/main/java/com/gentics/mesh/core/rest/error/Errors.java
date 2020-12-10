package com.gentics.mesh.core.rest.error;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;

/**
 * Central enum which keeps track of various common API errors. The {@link Errors#error(HttpResponseStatus, String, String...)} methods can be used to quickly
 * generate specific API errors.
 */
public enum Errors {

	NAME_CONFLICT(NameConflictException.TYPE, NameConflictException.class),

	GENERAL_ERROR(GenericRestException.TYPE, GenericRestException.class),

	NODE_VERSION_CONFLICT(NodeVersionConflictException.TYPE, NodeVersionConflictException.class);

	private String type;
	private Class<?> clazz;

	private Errors(String type, Class<?> clazz) {
		this.type = type;
		this.clazz = clazz;
	}

	public String getType() {
		return type;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 * @param i18nMessageKey
	 *            I18n key
	 * @param parameters
	 *            I18n message parameters
	 * @return
	 */
	public static NameConflictException conflict(String conflictingUuid, String conflictingName, String i18nMessageKey, String... parameters) {
		NameConflictException error = new NameConflictException(i18nMessageKey, parameters);
		error.setProperty("conflictingUuid", conflictingUuid);
		error.setProperty("conflictingName", conflictingName);
		return error;
	}

	/**
	 * Create a new http conflict exception.
	 * 
	 * @param conflictingUuid
	 *            Uuid of the object which was part of the conflict
	 * @param conflictingName
	 *            Name field value which caused the conflict
	 * @param conflictingLanguage
	 *            Language which caused the conflict
	 * @param i18nMessageKey
	 *            I18n key
	 * @param parameters
	 *            I18n message parameters
	 * @return
	 */
	public static NameConflictException nodeConflict(String conflictingUuid, String conflictingName, String conflictingLanguage,
		String i18nMessageKey, String... parameters) {
		NameConflictException error = new NameConflictException(i18nMessageKey, parameters);
		error.setProperty("conflictingUuid", conflictingUuid);
		error.setProperty("conflictingName", conflictingName);
		error.setProperty("conflictingLanguage", conflictingLanguage);
		return error;
	}

	/**
	 * Create an i18n translated error exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param parameters
	 *            i18n parameters
	 * @return
	 */
	public static GenericRestException error(HttpResponseStatus status, String i18nMessageKey, String... parameters) {
		return new GenericRestException(status, i18nMessageKey, parameters);
	}

	/**
	 * Create an i18n translated error single.
	 * 
	 * @param status
	 * @param i18nMessageKey
	 * @param parameters
	 * @return
	 */
	public static <T> Single<T> rxError(HttpResponseStatus status, String i18nMessageKey, String... parameters) {
		return Single.error(error(status, i18nMessageKey, parameters));
	}

	/**
	 * Create an i18n translated permission error exception.
	 * 
	 * @param elementType
	 * @param elementDescription
	 * @return
	 */
	public static PermissionException missingPerm(String elementType, String elementDescription) {
		return new PermissionException(elementType, elementDescription);
	}

	/**
	 * Create an i18n translated error exception.
	 * 
	 * @param status
	 *            Http status
	 * @param i18nMessageKey
	 *            i18n key
	 * @param t
	 *            Nested exception
	 * @return
	 */
	public static GenericRestException error(HttpResponseStatus status, String i18nMessageKey, Throwable t) {
		return new GenericRestException(status, i18nMessageKey, t);
	}

	/**
	 * Resolve the given typeName to a registered type.
	 * 
	 * @param typeName
	 * @return
	 */
	public static Errors valueByName(String typeName) {
		for (Errors type : values()) {
			if (type.getType()
				.equals(typeName)) {
				return type;
			}
		}
		return null;
	}

}
