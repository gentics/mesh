package com.gentics.mesh.graphdb.wrapper;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mda.entity.AUser;
import com.gentics.mda.entitycollection.UserDao;
import com.gentics.mda.page.ATransformablePage;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;

public class UserRootWrapper implements UserDao {
	private final UserRoot userRoot;

	public UserRootWrapper(UserRoot userRoot) {
		this.userRoot = userRoot;
	}

	@Override
	public UserRoot getDelegate() {
		return userRoot;
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return userRoot.findMeshAuthUserByUsername(username);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		return userRoot.findMeshAuthUserByUuid(userUuid);
	}

	@Override
	public AUser findByUsername(String username) {
		return new UserWrapper(userRoot.findByUsername(username));
	}

	@Override
	public void removeUser(AUser user) {
		userRoot.removeUser(user.getDelegate());
	}

	@Override
	public TraversalResult<? extends AUser> findAll() {
		return userRoot.findAll().map(UserWrapper::new);
	}

	@Override
	public Stream<? extends AUser> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return userRoot.findAllStream(ac, permission).map(UserWrapper::new);
	}

	@Override
	public TraversalResult<? extends AUser> findAllDynamic() {
		return userRoot.findAllDynamic().map(UserWrapper::new);
	}

	@Override
	public ATransformablePage<? extends AUser> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return userRoot.findAll(ac, pagingInfo).map(UserWrapper::new);
	}

	@Override
	public ATransformablePage<? extends AUser> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return userRoot.findAllNoPerm(ac, pagingInfo).map(UserWrapper::new);
	}

	@Override
	public AUser findByName(String name) {
		return new UserWrapper(userRoot.findByName(name));
	}

	@Override
	public AUser findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return new UserWrapper(userRoot.findByName(ac, name, perm));
	}

	@Override
	public AUser findByUuid(String uuid) {
		return new UserWrapper(userRoot.findByUuid(uuid));
	}

	@Override
	public AUser loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return new UserWrapper(userRoot.loadObjectByUuid(ac, uuid, perm));
	}

	@Override
	public AUser loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return new UserWrapper(userRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}

	@Override
	public AUser loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return new UserWrapper(userRoot.loadObjectByUuidNoPerm(uuid, errorIfNotFound));
	}

	@Override
	public AUser create(InternalActionContext ac, EventQueueBatch batch) {
		return new UserWrapper(userRoot.create(ac, batch));
	}

	@Override
	public AUser create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return new UserWrapper(userRoot.create(ac, batch, uuid));
	}

	@Override
	public String getRootLabel() {
		return userRoot.getRootLabel();
	}

	@Override
	public Class<? extends AUser> getPersistanceClass() {
		return UserWrapper.class;
	}

	@Override
	public long computeCount() {
		return userRoot.computeCount();
	}

	public Vertex getVertex() {
		return userRoot.getVertex();
	}

	public void delete(BulkActionContext bac) {
		userRoot.delete(bac);
	}

	public void delete() {
		userRoot.delete();
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		userRoot.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public boolean hasPublishPermissions() {
		return userRoot.hasPublishPermissions();
	}

	public void setCachedUuid(String uuid) {
		userRoot.setCachedUuid(uuid);
	}

	@Override
	public void setUuid(String uuid) {
		userRoot.setUuid(uuid);
	}

	@Override
	public String getUuid() {
		return userRoot.getUuid();
	}

	@Override
	public String getElementVersion() {
		return userRoot.getElementVersion();
	}

	@Override
	public Database db() {
		return userRoot.db();
	}

	@Override
	public Vertx vertx() {
		return userRoot.vertx();
	}

	@Override
	public MeshOptions options() {
		return userRoot.options();
	}

	@Override
	public AUser create(String username, AUser creator, String uuid) {
		return new UserWrapper(userRoot.create(username, creator.getDelegate(), uuid));
	}

	@Override
	public ATransformablePage<? extends AUser> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<AUser> extraFilter) {
		return userRoot.findAll(ac, pagingInfo, user -> extraFilter.test(new UserWrapper(user))).map(UserWrapper::new);
	}

	@Override
	public void addItem(AUser item) {
		userRoot.addItem(item.getDelegate());
	}

	@Override
	public void removeItem(AUser item) {
		userRoot.removeItem(item.getDelegate());
	}
}
