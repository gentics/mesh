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
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.tinkerpop.blueprints.Vertex;

public class UserRootWrapper extends MeshElementWrapper<UserRoot> implements UserDao {

	public UserRootWrapper(UserRoot delegate) {
		super(delegate);
	}

	@Override
	public UserRoot getDelegate() {
		return delegate;
	}

	@Override
	public UserRoot getDelegate() {
		return userRoot;
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUsername(String username) {
		return delegate.findMeshAuthUserByUsername(username);
	}

	@Override
	public MeshAuthUser findMeshAuthUserByUuid(String userUuid) {
		return delegate.findMeshAuthUserByUuid(userUuid);
	}

	@Override
	public AUser findByUsername(String username) {
		return UserWrapper.of(delegate.findByUsername(username));
	}

	@Override
	public void removeUser(AUser user) {
		delegate.removeUser(AUser.getDelegate(user));
	}

	@Override
	public TraversalResult<? extends AUser> findAll() {
		return delegate.findAll().map(UserWrapper::of);
	}

	@Override
	public Stream<? extends AUser> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return delegate.findAllStream(ac, permission).map(UserWrapper::of);
	}

	@Override
	public TraversalResult<? extends AUser> findAllDynamic() {
		return delegate.findAllDynamic().map(UserWrapper::of);
	}

	@Override
	public ATransformablePage<? extends AUser> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return delegate.findAll(ac, pagingInfo).map(UserWrapper::of);
	}

	@Override
	public ATransformablePage<? extends AUser> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return delegate.findAllNoPerm(ac, pagingInfo).map(UserWrapper::of);
	}

	@Override
	public AUser findByName(String name) {
		return UserWrapper.of(delegate.findByName(name));
	}

	@Override
	public AUser findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return UserWrapper.of(delegate.findByName(ac, name, perm));
	}

	@Override
	public AUser findByUuid(String uuid) {
		return UserWrapper.of(delegate.findByUuid(uuid));
	}

	@Override
	public AUser loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return UserWrapper.of(delegate.loadObjectByUuid(ac, uuid, perm));
	}

	@Override
	public AUser loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return UserWrapper.of(delegate.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}

	@Override
	public AUser loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return UserWrapper.of(delegate.loadObjectByUuidNoPerm(uuid, errorIfNotFound));
	}

	@Override
	public AUser create(InternalActionContext ac, EventQueueBatch batch) {
		return UserWrapper.of(delegate.create(ac, batch));
	}

	@Override
	public AUser create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return UserWrapper.of(delegate.create(ac, batch, uuid));
	}

	@Override
	public String getRootLabel() {
		return delegate.getRootLabel();
	}

	@Override
	public Class<? extends AUser> getPersistanceClass() {
		return UserWrapper.class;
	}

	@Override
	public long computeCount() {
		return delegate.computeCount();
	}

	public Vertex getVertex() {
		return delegate.getVertex();
	}

	public void delete(BulkActionContext bac) {
		delegate.delete(bac);
	}

	public void delete() {
		delegate.delete();
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant, Set<GraphPermission> permissionsToRevoke) {
		delegate.applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public boolean hasPublishPermissions() {
		return delegate.hasPublishPermissions();
	}

	public void setCachedUuid(String uuid) {
		delegate.setCachedUuid(uuid);
	}

	@Override
	public AUser create(String username, AUser creator, String uuid) {
		return UserWrapper.of(delegate.create(username, AUser.getDelegate(creator), uuid));
	}

	@Override
	public ATransformablePage<? extends AUser> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<AUser> extraFilter) {
		return delegate.findAll(ac, pagingInfo, user -> extraFilter.test(UserWrapper.of(user))).map(UserWrapper::of);
	}

	@Override
	public void addItem(AUser item) {
		delegate.addItem(AUser.getDelegate(item));
	}

	@Override
	public void removeItem(AUser item) {
		delegate.removeItem(AUser.getDelegate(item));
	}
}
