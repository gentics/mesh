package com.gentics.mesh.handler;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.handler.impl.VertxWebActionContextImpl;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

public interface ActionContext {

	Map<String, Object> data();

	ActionContext put(String key, Object obj);

	<T> T get(String key);

	/**
	 * Create a action context using the routing context in order to extract needed parameters.
	 *
	 * @return the body handler
	 */
	static ActionContext create(RoutingContext rc) {
		return new VertxWebActionContextImpl(rc);
	}

	Project getProject();

	MeshAuthUser getUser();

	String getParameter(String string);

	void send(String body);

	String i18n(String i18nKey, String... parameters);

	MultiMap getParameters();

	String query();

	void fail(HttpResponseStatus status, String i18nKey, String... parameters);

	void fail(HttpResponseStatus status, String i18nKey, Throwable cause);

	<T> AsyncResult<T> failedFuture(HttpResponseStatus status, String i18nKey, Throwable cause);

	<T> AsyncResult<T> failedFuture(HttpResponseStatus status, String i18nKey, String... parameters);

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

}
