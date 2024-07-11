package com.gentics.mesh.hibernate.data.permission;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.dao.PermissionRoots;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.database.CurrentTransaction;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.PermissionType;
import com.gentics.mesh.hibernate.data.domain.HibPermissionRootImpl;
import com.gentics.mesh.hibernate.data.domain.HibProjectImpl;

/**
 * Permission roots manager component implementation.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibPermissionRoots implements PermissionRoots {

	private final CurrentTransaction currentTransaction;
	private HibPermissionRootImpl project;
	private HibPermissionRootImpl user;
	private HibPermissionRootImpl group;
	private HibPermissionRootImpl role;
	private HibPermissionRootImpl microschema;
	private HibPermissionRootImpl schema;
	private HibPermissionRootImpl mesh;

	@Inject
	public HibPermissionRoots(CurrentTransaction currentTransaction) {
		this.currentTransaction = currentTransaction;
	}

	@Override
	public BaseElement project() {
		project = lazyGet(project, PermissionType.PROJECT);
		return project;
	}

	@Override
	public BaseElement user() {
		user = lazyGet(user, PermissionType.USER);
		return user;
	}

	@Override
	public BaseElement group() {
		group = lazyGet(group, PermissionType.GROUP);
		return group;
	}

	@Override
	public BaseElement role() {
		role = lazyGet(role, PermissionType.ROLE);
		return role;
	}

	@Override
	public BaseElement microschema() {
		microschema = lazyGet(microschema, PermissionType.MICROSCHEMA);
		return microschema;
	}

	@Override
	public BaseElement schema() {
		schema = lazyGet(schema, PermissionType.SCHEMA);
		return schema;
	}

	@Override
	public BaseElement mesh() {
		mesh = lazyGet(mesh, PermissionType.MESH);
		return mesh;
	}

	public BaseElement buildPermissionRootWithParent(Project parent, String urlPath) {
		PermissionType permissionType = PermissionType.fromUrlStringPath(urlPath);
		return buildPermissionRootWithParent(parent, permissionType);
	}

	public BaseElement buildPermissionRootWithParent(Project parent, PermissionType permissionType) {
		return currentTransaction.getEntityManager()
				.createQuery("from permissionroot where type = :type and parent = :parent", HibPermissionRootImpl.class)
				.setParameter("type", permissionType)
				.setParameter("parent", parent)
				.getResultStream()
				.findFirst()
				.orElseGet(() -> createPermissionRoot(parent, permissionType));
	}

	private HibPermissionRootImpl createPermissionRoot(Project root, PermissionType permissionType) {
		HibernateTx hibTx = HibernateTx.get();
		HibPermissionRootImpl permissionRoot = hibTx.create(HibPermissionRootImpl.class);
		permissionRoot.setType(permissionType);
		permissionRoot.setParent(root);
		currentTransaction.getEntityManager().persist(permissionRoot);
		((HibProjectImpl) root).addPermissionRoot(permissionRoot);

		return permissionRoot;
	}

	private HibPermissionRootImpl lazyGet(HibPermissionRootImpl permission, PermissionType elementType) {
		if (permission == null) {
			synchronized (HibPermissionRoots.class) {
				if (permission == null) {
					return currentTransaction.getEntityManager()
							.createQuery("from permissionroot where type = :type and parent is null", HibPermissionRootImpl.class)
							.setParameter("type", elementType)
							.getResultStream()
							.findFirst()
							.orElseGet(() -> {
								HibernateTx hibTx = HibernateTx.get();
								HibPermissionRootImpl newPermission = hibTx.create(HibPermissionRootImpl.class);
								newPermission.setType(elementType);
								currentTransaction.getEntityManager().persist(newPermission);
								return newPermission;
							});
				}
			}
		}
		return permission;
	}
}
