//package com.gentics.mesh.core.data.impl;
//
//import java.util.Set;
//
//import com.gentics.mesh.core.data.perm.InternalPermission;
//import com.gentics.mesh.core.data.schema.HibMicroschema;
//import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
//import com.gentics.mesh.core.data.schema.HibSchemaChange;
//import com.gentics.mesh.core.data.schema.MicroschemaVersion;
//import com.gentics.mesh.core.data.schema.SchemaChange;
//import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
//import com.gentics.mesh.core.rest.schema.MicroschemaReference;
//
//public class MicroschemaVersionWrapper implements HibMicroschemaVersion {
//
//	private MicroschemaVersion delegate;
//
//	public static MicroschemaVersionWrapper wrap(MicroschemaVersion version) {
//		if (version == null) {
//			return null;
//		} else {
//			return new MicroschemaVersionWrapper(version);
//		}
//	}
//
//	public MicroschemaVersionWrapper(MicroschemaVersion delegate) {
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
//	public MicroschemaVersionModel getSchema() {
//		return delegate.getSchema();
//	}
//
//	@Override
//	public void setSchema(MicroschemaVersionModel schema) {
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
//	public boolean hasPublishPermissions() {
//		return delegate.hasPublishPermissions();
//	}
//
//	@Override
//	public MicroschemaReference transformToReference() {
//		return delegate.transformToReference();
//	}
//
//	@Override
//	public HibMicroschema getSchemaContainer() {
//		return delegate.getSchemaContainer();
//	}
//
//	@Override
//	public HibMicroschemaVersion getPreviousVersion() {
//		return wrap(delegate.getPreviousVersion());
//	}
//
//	@Override
//	public HibMicroschemaVersion getNextVersion() {
//		return wrap(delegate.getNextVersion());
//	}
//
//	@Override
//	public void setNextVersion(HibMicroschemaVersion version) {
//		delegate.setNextVersion(unwrap(version));
//	}
//
//	@Override
//	public HibSchemaChange<?> getPreviousChange() {
//		return delegate.getPreviousChange();
//	}
//
//	@Override
//	public void setPreviousChange(HibSchemaChange<?> change) {
//		delegate.setPreviousChange(change);
//	}
//}
