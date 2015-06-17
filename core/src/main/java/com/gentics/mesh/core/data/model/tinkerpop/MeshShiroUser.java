package com.gentics.mesh.core.data.model.tinkerpop;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.impl.SimplePrincipalCollection;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

public class MeshShiroUser extends MeshUser implements ClusterSerializable, User {

	private Vertx vertx;
	private org.apache.shiro.mgt.SecurityManager securityManager;
	private String username;
	private Subject subject;
	private JsonObject principal;
	private String rolePrefix;

	public MeshShiroUser(Vertx vertx, org.apache.shiro.mgt.SecurityManager securityManager, String username, String rolePrefix) {
		this.vertx = vertx;
		this.securityManager = securityManager;
		this.username = username;
		this.rolePrefix = rolePrefix;
		setSubject();
	}

	public MeshShiroUser() {
	}

	@Override
	public JsonObject principal() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		// TODO Auto-generated method stub

	}

	@Override
	public User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public User clearCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void writeToBuffer(Buffer buffer) {
		// TODO Auto-generated method stub

	}

	@Override
	public int readFromBuffer(int pos, Buffer buffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	private void setSubject() {
		SubjectContext subjectContext = new DefaultSubjectContext();
		PrincipalCollection coll = new SimplePrincipalCollection(username);
		subjectContext.setPrincipals(coll);
		subject = securityManager.createSubject(subjectContext);
	}
}
