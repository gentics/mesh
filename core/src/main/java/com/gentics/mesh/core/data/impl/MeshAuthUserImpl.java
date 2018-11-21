package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.syncleus.ferma.traversals.VertexTraversal;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;

/**
 * @see MeshAuthUser
 */
public class MeshAuthUserImpl extends UserImpl implements MeshAuthUser {

	private String uuid;

	@Override
	public String getUuid() {
		// Cache the user uuid so that we can reuse it during the login process
		if (uuid != null) {
			return uuid;
		} else {
			uuid = super.getUuid();
		}
		return uuid;
	}

	/**
	 * An active transaction is required in order to load the json data.
	 */
	@Override
	public JsonObject principal() {
		JsonObject user = new JsonObject();
		user.put("uuid", getUuid());
		user.put("username", getUsername());
		user.put("firstname", getFirstname());
		user.put("lastname", getLastname());
		user.put("emailAddress", getEmailAddress());

		JsonArray rolesArray = new JsonArray();
		user.put("roles", rolesArray);
		for (Role role : getRoles()) {
			JsonObject roleJson = new JsonObject();
			roleJson.put("uuid", role.getUuid());
			roleJson.put("name", role.getName());
			rolesArray.add(roleJson);
		}

		JsonArray groupsArray = new JsonArray();
		user.put("groups", groupsArray);
		for (Group group : getGroups()) {
			JsonObject groupJson = new JsonObject();
			groupJson.put("uuid", group.getUuid());
			groupJson.put("name", group.getName());
			groupsArray.add(groupJson);
		}

		Node reference = getReferencedNode();
		if (reference != null) {
			user.put("nodeReference", reference.getUuid());
		}
		return user;
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

	@Override
	public VertexTraversal<?, ?, ?> getPermTraversal(GraphPermission permission) {
		return out(HAS_USER).in(HAS_ROLE).out(permission.label());
	}

}