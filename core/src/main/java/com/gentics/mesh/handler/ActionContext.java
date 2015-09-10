package com.gentics.mesh.handler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.handler.impl.HttpActionContextImpl;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;

/**
 * Abstraction of the vertx-web routing context.
 */
public interface ActionContext {

	Map<String, Object> data();

	ActionContext put(String key, Object obj);

	<T> T get(String key);

	//TODO we should get this of this and make it possible to choose between httmlaction context and internal context
	/**
	 * Create a action context using the routing context in order to extract needed parameters.
	 *
	 * @return the body handler
	 */
	static ActionContext create(RoutingContext rc) {
		return new HttpActionContextImpl(rc);
	}

	Project getProject();

	MeshAuthUser getUser();

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
	 */
	void send(String body);

	/**
	 * Return the i18n string for the given i18n key and the parameters. This method is a wrapper that will lookup the defined locale and return a matching i18n
	 * translation.
	 * 
	 * @param i18nKey
	 * @param parameters
	 * @return
	 */
	String i18n(String i18nKey, String... parameters);

	MultiMap getParameters();

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

	<T> AsyncResult<T> failedFuture(HttpResponseStatus status, String i18nKey, Throwable cause);

	<T> AsyncResult<T> failedFuture(HttpResponseStatus status, String i18nKey, String... parameters);

	/**
	 * Fail the action with the given cause.
	 * 
	 * @param cause
	 */
	void fail(Throwable cause);

	/**
	 * Extracts the lang parameter values from the query.
	 * 
	 * @return List of languages. List can be empty.
	 */
	List<String> getSelectedLanguageTags();

	/**
	 * Extract the paging information from the request parameters. The paging information contains information about the number of the page that is currently
	 * requested and the amount of items that should be included in a single page.
	 * 
	 * @return Paging information
	 */
	PagingInfo getPagingInfo();

	<T> T fromJson(Class<?> classOfT) throws HttpStatusCodeErrorException;

	String getBodyAsString();

	Locale getLocale();

	List<String> getExpandedFieldnames();

	void setUser(User user);

	/**
	 * Perform a logout.
	 */
	void logout();

}
