package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;
import static com.gentics.mesh.etc.MeshSpringConfiguration.getMeshSpringConfiguration;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.relationship.Permission;
import com.syncleus.ferma.traversals.VertexTraversal;

public class MeshAuthUserImpl extends MeshUserImpl implements ClusterSerializable, User, MeshAuthUser {

	@Override
	public JsonObject principal() {
		throw new NotImplementedException();
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		throw new NotImplementedException();
	}

	@Override
	public User isAuthorised(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Please use the MeshShiroUser method instead.");
	}

	public MeshAuthUserImpl isAuthorised(MeshVertex targetNode, Permission permission, Handler<AsyncResult<Boolean>> resultHandler) {
		final MeshAuthUserImpl user = this;
		getMeshSpringConfiguration().vertx().executeBlocking(fut -> fut.complete(user.hasPermission(targetNode, permission)), resultHandler);
		return this;
	}

	@Override
	public User clearCache() {
		throw new NotImplementedException();
	}

	@Override
	public void writeToBuffer(Buffer buffer) {
		throw new NotImplementedException();
	}

	@Override
	public int readFromBuffer(int pos, Buffer buffer) {
		throw new NotImplementedException();
	}

	public VertexTraversal<?, ?, ?> getPermTraversal(Permission permission) {
		// TODO out/in/out!
		return out(HAS_USER).in(HAS_ROLE).out(permission.label());
	}

	public String getPermissionNames(Tag tag) {
		return null;
	}

	@Override
	public MeshAuthUserImpl getImpl() {
		return this;
	}


}