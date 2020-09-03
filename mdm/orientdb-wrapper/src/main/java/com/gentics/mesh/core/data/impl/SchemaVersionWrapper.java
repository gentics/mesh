//package com.gentics.mesh.core.data.impl;
//
//import java.util.Set;
//
//import com.gentics.mesh.core.data.job.HibJob;
//import com.gentics.mesh.core.data.perm.InternalPermission;
//import com.gentics.mesh.core.data.schema.HibSchema;
//import com.gentics.mesh.core.data.schema.HibSchemaChange;
//import com.gentics.mesh.core.data.schema.HibSchemaVersion;
//import com.gentics.mesh.core.data.schema.SchemaChange;
//import com.gentics.mesh.core.data.schema.SchemaVersion;
//import com.gentics.mesh.core.rest.schema.SchemaReference;
//import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
//
//public class SchemaVersionWrapper implements HibSchemaVersion {
//
//	private SchemaVersion delegate;
//
//	public static SchemaVersionWrapper wrap(SchemaVersion version) {
//		if (version == null) {
//			return null;
//		} else {
//			return new SchemaVersionWrapper(version);
//		}
//	}
//
//	public SchemaVersionWrapper(SchemaVersion delegate) {
//		this.delegate = delegate;
//	}
//
//	@Override
//	public String getName() {
//		return delegate.getName();
//	}
//
//	@Override
//	public String getJson() {
//		return delegate.getJson();
//	}
//
//	@Override
//	public void setJson(String json) {
//		delegate.setJson(json);
//	}
//
//	@Override
//	public String getVersion() {
//		return delegate.getVersion();
//	}
//
//	@Override
//	public SchemaVersionModel getSchema() {
//		return delegate.getSchema();
//	}
//
//	@Override
//	public void setSchema(SchemaVersionModel schema) {
//		delegate.setSchema(schema);
//	}
//
//	@Override
//	public SchemaChange<?> getNextChange() {
//		return delegate.getNextChange();
//	}
//
//	@Override
//	public void setNextChange(HibSchemaChange<?> change) {
//		delegate.setNextChange(change);
//	}
//
//	@Override
//	public void deleteElement() {
//		delegate.remove();
//	}
//
//	@Override
//	public String getUuid() {
//		return delegate.getUuid();
//	}
//
//	@Override
//	public Object getId() {
//		return delegate.getId();
//	}
//
//	@Override
//	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
//		return delegate.getRoleUuidsForPerm(permission);
//	}
//
//	@Override
//	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
//		delegate.setRoleUuidForPerm(permission, allowedRoles);
//	}
//
//	@Override
//	public HibSchemaVersion getPreviousVersion() {
//		return wrap(delegate.getPreviousVersion());
//	}
//
//	@Override
//	public HibSchemaVersion getNextVersion() {
//		return wrap(delegate.getNextVersion());
//	}
//
//	@Override
//	public HibSchema getSchemaContainer() {
//		return delegate.getSchemaContainer();
//	}
//
//	@Override
//	public SchemaReference transformToReference() {
//		return delegate.transformToReference();
//	}
//
//	@Override
//	public String getElementVersion() {
//		return delegate.getElementVersion();
//	}
//
//	@Override
//	public Iterable<? extends HibJob> referencedJobsViaTo() {
//		return delegate.referencedJobsViaFrom();
//	}
//
//	@Override
//	public SchemaChange<?> getPreviousChange() {
//		return delegate.getPreviousChange();
//	}
//
//}
