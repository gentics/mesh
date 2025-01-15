package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.user.MeshAuthUser;

/**
 * @see UserRoot
 */
public class UserRootImpl extends AbstractRootVertex<User> implements UserRoot {

	/**
	 * Initialise the type and indices for this type.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(UserRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_USER).withInOut().withOut());
	}

	@Override
	public Class<? extends User> getPersistanceClass() {
		return UserImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_USER;
	}

	@Override
	public void addUser(User user) {
		addItem(user);
	}

	@Override
	public void removeUser(User user) {
		removeItem(user);
	}

	/**
	 * Redirected to {@link #findByUsername(String)}
	 */
	@Override
	public User findByName(String name) {
		return findByUsername(name);
	}

	@Override
	public User findByUsername(String username) {
		return (User) mesh().userNameCache().get(username, name -> {
			return out(HAS_USER).has(UserImpl.USERNAME_PROPERTY_KEY, name).nextOrDefaultExplicit(UserImpl.class, null);
		});
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return MeshAuthUserImpl.create(
			db(),
			findByUsername(username)
		);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		return MeshAuthUserImpl.create(
			db(),
			findByUuid(userUuid)
		);
	}

	@Override
	public String mapGraphQlFieldNameForSorting(String gqlName) {
		switch (gqlName) {
		case "name": return "username";
		}
		return super.mapGraphQlFieldNameForSorting(gqlName);
	}

	@Override
	public void delete() {
		throw new NotImplementedException("The user root should never be deleted");
	}
}
