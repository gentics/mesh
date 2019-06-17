package com.gentics.mesh.handler;

import static com.gentics.mesh.http.HttpConstants.APPLICATION_JSON_UTF8;

import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.core.rest.error.GenericRestException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;

/**
 * Abstraction of the vertx-web routing context.
 */
public interface ActionContext {

	/**
	 * Return the data map that is bound to this context.
	 * 
	 * @return Data map
	 */
	Map<String, Object> data();

	/**
	 * Add the data object for the given key to the data map.
	 * 
	 * @param key
	 *            Data key
	 * @param obj
	 *            Data object
	 * @return Fluent API
	 */
	ActionContext put(String key, Object obj);

	/**
	 * Return the data object for the given key.
	 * 
	 * @param key
	 *            Data key
	 * @return Data value or null when no value could be found for the given key
	 */
	<T> T get(String key);

	/**
	 * Return the request parameter with the given name.
	 * 
	 * @param name
	 *            Name of the request parameter
	 * @return value of the request parameter or null if the parameter was not found
	 */
	String getParameter(String name);

	/**
	 * Returns the action context request parameters.
	 * 
	 * @return
	 */
	MultiMap getParameters();

	/**
	 * Set request context specific path parameters.
	 * 
	 * @param name
	 * @param value
	 */
	void setParameter(String name, String value);

	/**
	 * Send the body string and complete the action.
	 * 
	 * @param body
	 *            the body string that should be send
	 * @param statusCode
	 *            the status code to send
	 */
	default void send(String body, HttpResponseStatus statusCode) {
		send(body, statusCode, APPLICATION_JSON_UTF8);
	}

	/**
	 * Send the body string and complete the action with a status code of 200 OK.
	 * 
	 * @param body
	 *            the body string that should be send
	 */
	default void send(String body) {
		this.send(body, HttpResponseStatus.OK);
	}

	/**
	 * Send the body string with the given status code and contentType.
	 * 
	 * @param body
	 * @param status
	 * @param contentType
	 */
	void send(String body, HttpResponseStatus status, String contentType);

	/**
	 * Return the i18n string for the given i18n key and the parameters. This method is a wrapper that will lookup the defined locale and return a matching i18n
	 * translation.
	 * 
	 * @param i18nKey
	 *            I18n message key
	 * @param parameters
	 *            I18n message parameters
	 * @return
	 */
	String i18n(String i18nKey, String... parameters);

	/**
	 * Return the query string.
	 * 
	 * @return Query string
	 */
	String query();

	/**
	 * Split the query up and provide a map with key value sets for each parameter.
	 * 
	 * @return
	 */
	Map<String, String> splitQuery();

	/**
	 * Fail the action with the given cause.
	 * 
	 * @param cause
	 *            Failure
	 */
	void fail(Throwable cause);

	/**
	 * Deserialize the body string using the given class.
	 * 
	 * @param classOfT
	 *            Class to be used for deserialisation
	 * @return Deserialized object
	 * @throws GenericRestException
	 */
	<T> T fromJson(Class<?> classOfT) throws GenericRestException;

	/**
	 * Return the body string of the request.
	 * 
	 * @return Body string
	 */
	String getBodyAsString();

	/**
	 * Return the current set locale.
	 * 
	 * @return Locale
	 */
	Locale getLocale();

	/**
	 * Perform a logout.
	 */
	void logout();


}
