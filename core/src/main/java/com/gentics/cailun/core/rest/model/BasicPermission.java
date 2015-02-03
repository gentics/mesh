package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class BasicPermission extends GenericPermission {

	public BasicPermission(GenericNode targetObject, BasicPermissionTypes type) {
		super(targetObject, type.name());
	}

	// public GenericPermission getCreatePermission() {
	// // lazy loading/update of transient field
	// if (canCreate && createPermission == null) {
	// createPermission = new GenericPermission(object, CREATE);
	// }
	// return createPermission;
	// }
	//
	// public GenericPermission getDeletePermission() {
	// // lazy loading/update of transient field
	// if (canDelete && deletePermission == null) {
	// deletePermission = new GenericPermission(object, DELETE);
	// }
	// return deletePermission;
	// }
	//
	// public GenericPermission getModifyPermission() {
	// // lazy loading/update of transient field
	// if (canModify && modifyPermission == null) {
	// modifyPermission = new GenericPermission(object, MODIFY);
	// }
	//
	// return modifyPermission;
	// }
	//
	// public GenericPermission getReadPermission() {
	// // lazy loading/update of transient field
	// if (canRead && readPermission == null) {
	// readPermission = new GenericPermission(object, READ);
	// }
	// return readPermission;
	// }

	// public Set<GenericPermission> getAllSetPermissions() {
	// Set<GenericPermission> set = new HashSet<>();
	// if (getCreatePermission() != null) {
	// set.add(getCreatePermission());
	// }
	// if (getModifyPermission() != null) {
	// set.add(getModifyPermission());
	// }
	// if (getDeletePermission() != null) {
	// set.add(getDeletePermission());
	// }
	// if (getReadPermission() != null) {
	// set.add(getReadPermission());
	// }
	// return set;
	// }
	//
	// public void setCanRead(boolean b) {
	// boolean old = canRead;
	// canRead = b;
	// if (old != canRead && canRead == true) {
	// readPermission = new GenericPermission(object, READ);
	// }
	// if (canRead == false) {
	// readPermission = null;
	// }
	// }
	//
	// public void setCanModify(boolean b) {
	// boolean old = canModify;
	// canModify = b;
	// if (old != canModify && canModify == true) {
	// modifyPermission = new GenericPermission(object, MODIFY);
	// }
	// if (canModify == false) {
	// modifyPermission = null;
	// }
	// }
	//
	// public void setCanDelete(boolean b) {
	// boolean old = canDelete;
	// canDelete = b;
	// if (old != canDelete && canDelete == true) {
	// deletePermission = new GenericPermission(object, DELETE);
	// }
	// if (canDelete == false) {
	// deletePermission = null;
	// }
	// }
	//
	// public void setCanCreate(boolean b) {
	// boolean old = canCreate;
	// canCreate = b;
	// if (old != canCreate && canCreate == true) {
	// createPermission = new GenericPermission(object, CREATE);
	// }
	// if (canRead == false) {
	// createPermission = null;
	// }
	// }
}
