package com.gentics.mesh.handler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.MultiMap;

/**
 * Abstraction of the vertx-web routing context.
 */
public interface ActionContext {

	/**
	 * Return the data map that is bound to this context.
	 * 
	 * @return
	 */
	Map<String, Object> data();

	/**
	 * Add the data object for the given key to the data map.
	 * 
	 * @param key
	 * @param obj
	 * @return Fluent API
	 */
	ActionContext put(String key, Object obj);

	/**
	 * Return the data object for the given key.
	 * 
	 * @param key
	 * @return
	 */
	<T> T get(String key);

	/**
	 * Return the request parameter with the given name.
	 * 
	 * @param string
	 * @return
	 */
	String getParameter(String name);

	/**
	 * Send the body string and complete the action.
	 * 
	 * @param body
	 *            the body string that should be send
	 * @param statusCode
	 *            the status code to send
	 */
	void send(String body, HttpResponseStatus statusCode);
	
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
	 * Return the i18n string for the given i18n key and the parameters. This method is a wrapper that will lookup the defined locale and return a matching i18n
	 * translation.
	 * 
	 * @param i18nKey
	 * @param parameters
	 * @return
	 */
	String i18n(String i18nKey, String... parameters);

	/**
	 * Return the request parameters.
	 * 
	 * @return
	 */
	MultiMap getParameters();

	/**
	 * Return the query string.
	 * 
	 * @return
	 */
	String query();

	/**
	 * Fail the action with the given status and return a generic message response which includes the given i18n message.
	 * 
	 * @param status
	 * @param i18nKey
	 * @param parameters
	 */
	void fail(HttpResponseStatus status, String i18nKey, String... parameters);

	/**
	 * Fail the action with the given status and return a generic message response which includes the given i18n message and cause.
	 * 
	 * @param status
	 * @param i18nKey
	 * @param cause
	 */
	void fail(HttpResponseStatus status, String i18nKey, Throwable cause);

	/**
	 * Fail the action with the given cause.
	 * 
	 * @param cause
	 */
	void fail(Throwable cause);

	/**
	 * Deserialize the body string using the given class.
	 * 
	 * @param classOfT
	 * @return
	 * @throws HttpStatusCodeErrorException
	 */
	<T> T fromJson(Class<?> classOfT) throws HttpStatusCodeErrorException;

	/**
	 * Return the body string of the request.
	 * 
	 * @return
	 */
	String getBodyAsString();

	/**
	 * Return the current set locale.
	 * 
	 * @return
	 */
	Locale getLocale();

	//TODO move this to internal action context
	/**
	 * Return a list of field name that should be expanded.
	 * 
	 * @return
	 */
	List<String> getExpandedFieldnames();

	/**
	 * Perform a logout.
	 */
	void logout();

}
