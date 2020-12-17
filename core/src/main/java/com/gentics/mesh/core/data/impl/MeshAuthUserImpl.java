package com.gentics.mesh.core.data.impl;

import java.util.Objects;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * Wraps a {@link HibUser} to implement {@link MeshAuthUser}.
 * 
 * @see MeshAuthUser
 */
public class MeshAuthUserImpl implements MeshAuthUser {

	private final Database db;
	private final HibUser delegate;

	private MeshAuthUserImpl(Database db, HibUser user) {
		this.db = db;
		this.delegate = user;
	}

	/**
	 * Create a new {@link MeshAuthUser} wrapper.
	 * 
	 * @param db
	 * @param user
	 * @return
	 */
	public static MeshAuthUserImpl create(Database db, HibUser user) {
		Objects.requireNonNull(db);
		if (user == null) {
			return null;
		}
		return new MeshAuthUserImpl(db, user);
	}

	/**
	 * An active transaction is required in order to load the json data.
	 */
	@Override
	public JsonObject principal() {
		return db.tx(tx -> {
			UserDaoWrapper userDao = tx.userDao();
			JsonObject user = new JsonObject();
			user.put("uuid", delegate.getUuid());
			user.put("username", delegate.getUsername());
			user.put("firstname", delegate.getFirstname());
			user.put("lastname", delegate.getLastname());
			user.put("emailAddress", delegate.getEmailAddress());
			user.put("admin", delegate.isAdmin());

			JsonArray rolesArray = new JsonArray();
			user.put("roles", rolesArray);
			for (HibRole role : userDao.getRoles(delegate)) {
				JsonObject roleJson = new JsonObject();
				roleJson.put("uuid", role.getUuid());
				roleJson.put("name", role.getName());
				rolesArray.add(roleJson);
			}

			JsonArray groupsArray = new JsonArray();
			user.put("groups", groupsArray);
			for (HibGroup group : userDao.getGroups(delegate)) {
				JsonObject groupJson = new JsonObject();
				groupJson.put("uuid", group.getUuid());
				groupJson.put("name", group.getName());
				groupsArray.add(groupJson);
			}

			HibNode reference = delegate.getReferencedNode();
			if (reference != null) {
				user.put("nodeReference", reference.getUuid());
			}
			return user;
		});
	}

	@Override
	public void setAuthProvider(AuthProvider authProvider) {
		throw new NotImplementedException();
	}

	@Override
	public User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
		throw new NotImplementedException("Please use the MeshAuthUserImpl method instead.");
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

	public HibUser getDelegate() {
		return delegate;
	}

}
