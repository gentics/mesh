package com.gentics.mesh.core.data.impl;

import java.util.Objects;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.authorization.Authorization;

/**
 * Wraps a {@link User} to implement {@link MeshAuthUser}.
 * 
 * @see MeshAuthUser
 */
public class MeshAuthUserImpl implements MeshAuthUser {

	private final Database db;
	private final User delegate;

	private MeshAuthUserImpl(Database db, User user) {
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
	public static MeshAuthUserImpl create(Database db, User user) {
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
			UserDao userDao = tx.userDao();
			JsonObject user = new JsonObject();
			user.put("uuid", delegate.getUuid());
			user.put("username", delegate.getUsername());
			user.put("firstname", delegate.getFirstname());
			user.put("lastname", delegate.getLastname());
			user.put("emailAddress", delegate.getEmailAddress());
			user.put("admin", delegate.isAdmin());

			JsonArray rolesArray = new JsonArray();
			user.put("roles", rolesArray);
			for (Role role : userDao.getRoles(delegate)) {
				JsonObject roleJson = new JsonObject();
				roleJson.put("uuid", role.getUuid());
				roleJson.put("name", role.getName());
				rolesArray.add(roleJson);
			}

			JsonArray groupsArray = new JsonArray();
			user.put("groups", groupsArray);
			for (Group group : userDao.getGroups(delegate)) {
				JsonObject groupJson = new JsonObject();
				groupJson.put("uuid", group.getUuid());
				groupJson.put("name", group.getName());
				groupsArray.add(groupJson);
			}

			Node reference = delegate.getReferencedNode();
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
	public io.vertx.ext.auth.User merge(io.vertx.ext.auth.User user) {
		throw new NotImplementedException();
	}

	@Override
	public JsonObject attributes() {
		throw new NotImplementedException();
	}

	@Override
	public io.vertx.ext.auth.User isAuthorized(Authorization authorization, Handler<AsyncResult<Boolean>> handler) {
		throw new NotImplementedException("Please use the MeshAuthUserImpl method instead.");
	}

	@Override
	public io.vertx.ext.auth.User clearCache() {
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

	public User getDelegate() {
		return delegate;
	}

}
