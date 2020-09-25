package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.event.EventQueueBatch;

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

	@Override
	public long globalCount() {
		return db().count(UserImpl.class);
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
		return out(HAS_USER).has(UserImpl.USERNAME_PROPERTY_KEY, username).nextOrDefaultExplicit(UserImpl.class, null);
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
	public UserResponse transformToRestSync(User user, InternalActionContext ac, int level, String... languageTags) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public void delete(User user, BulkActionContext bac) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public boolean update(User element, InternalActionContext ac, EventQueueBatch batch) {
		throw new RuntimeException("Wrong invocation. Use dao instead");
	}

	@Override
	public User create() {
		return getGraph().addFramedVertex(UserImpl.class);
	}

	@Override
	public void delete(BulkActionContext context) {
		throw new NotImplementedException("The user root should never be deleted");
	}

	@Override
	public User create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		throw new RuntimeException("Wrong invocation. Use Dao instead.");
	}

}
