package com.gentics.mesh.handler.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.link.WebRootLinkReplacer.Type;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.ImageManipulationParameter;
import com.gentics.mesh.query.impl.NavigationRequestParameter;
import com.gentics.mesh.query.impl.PagingParameter;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.User;

public class NodeMigrationActionContextImpl extends AbstractActionContext implements InternalActionContext {
	private Map<String, Object> data;

	private String body;

	private String query;

	private List<String> languageTags;

	/**
	 * Set the body
	 *
	 * @param body
	 */
	public void setBody(String body) {
		this.body = body;
	}

	/**
	 * Set the query
	 *
	 * @param query
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/**
	 * Set the language tags
	 *
	 * @param languageTags
	 */
	public void setLanguageTags(List<String> languageTags) {
		this.languageTags = languageTags;
	}

	@Override
	public Map<String, Object> data() {
		if (data == null) {
			data = new HashMap<>();
		}
		return data;
	}

	@Override
	public String getParameter(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void send(String body, HttpResponseStatus statusCode) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String query() {
		return query;
	}

	@Override
	public void fail(HttpResponseStatus status, String i18nKey, String... parameters) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fail(HttpResponseStatus status, String i18nKey, Throwable cause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fail(Throwable cause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getBodyAsString() {
		return body;
	}

	@Override
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logout() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setUser(User user) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Project getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Release getRelease() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MeshAuthUser getUser() {
		return null;
	}

	@Override
	public List<String> getSelectedLanguageTags() {
		return languageTags;
	}

	@Override
	public String getRolePermissionParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PagingParameter getPagingParameter() {
		return PagingParameter.fromQuery(query());
	}

	@Override
	public <T> Handler<AsyncResult<T>> errorHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Database getDatabase() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getExpandAllFlag() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Type getResolveLinksType() {
		return Type.OFF;
	}

	@Override
	public ImageManipulationParameter getImageRequestParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public NavigationRequestParameter getNavigationRequestParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void respond(RestModel result, HttpResponseStatus status) {
		// TODO Auto-generated method stub
		
	}

}
