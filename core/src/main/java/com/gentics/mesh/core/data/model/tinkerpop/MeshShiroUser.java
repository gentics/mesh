package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.impl.SimplePrincipalCollection;

import org.apache.commons.lang.NotImplementedException;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.subject.support.DefaultSubjectContext;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.syncleus.ferma.traversals.VertexTraversal;

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
		throw new NotImplementedException("Please use the MeshShiroUser method instead.");
	}

	public MeshShiroUser isAuthorised(MeshVertex targetNode, Permission permission, Handler<AsyncResult<Boolean>> resultHandler) {
		
//		  if (cachedPermissions.contains(authority)) {
//		      resultHandler.handle(Future.succeededFuture(true));
//		    } else {
//		      doIsPermitted(authority, res -> {
//		        if (res.succeeded()) {
//		          if (res.result()) {
//		            cachedPermissions.add(authority);
//		          }
//		        }
//		        resultHandler.handle(res);
//		      });
//		    }
//		    return this;
//		
		//vertx.executeBlocking(fut -> fut.complete(this.hasPermission(targetNode, permission)), resultHandler);
		return this;
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

	public VertexTraversal<?, ?, ?> getPermTraversal(Permission permission) {
		// TODO out/in/out?
		return in(HAS_USER).out(HAS_ROLE).out(permission.label());
	}

	public void setVertx(Vertx vertx) {
		this.vertx = vertx;

	}

}
