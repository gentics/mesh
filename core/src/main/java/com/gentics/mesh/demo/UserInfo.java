package com.gentics.mesh.demo;

import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;

/**
 * Container for user, group, role and password references of an user.
 */
public class UserInfo {

	private HibUser user;
	private String userUuid;

	private HibGroup group;
	private String groupUuid;

	private HibRole role;
	private String roleUuid;

	private String password;

	public UserInfo(HibUser user, HibGroup group, HibRole role, String password) {
		this.user = user;
		this.userUuid = user.getUuid();
		this.group = group;
		this.groupUuid = group.getUuid();
		this.role = role;
		this.roleUuid = role.getUuid();
		this.password = password;
	}

	public void setRole(HibRole role) {
		this.role = role;
	}

	public void setUser(HibUser user) {
		this.user = user;
	}

	public void setGroup(HibGroup group) {
		this.group = group;
	}

	public HibGroup getGroup() {
		Tx.maybeGet().ifPresent(tx -> {
			group = tx.<CommonTx>unwrap().load(group.getId(), tx.<CommonTx>unwrap().groupDao().getPersistenceClass());
		});
		return group;
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public HibRole getRole() {
		Tx.maybeGet().ifPresent(tx -> {
			role = tx.<CommonTx>unwrap().load(role.getId(), tx.<CommonTx>unwrap().roleDao().getPersistenceClass());
		});
		return role;
	}

	public String getRoleUuid() {
		return roleUuid;
	}

	public HibUser getUser() {
		Tx.maybeGet().ifPresent(tx -> {
			user = tx.<CommonTx>unwrap().load(user.getId(), tx.<CommonTx>unwrap().userDao().getPersistenceClass());
		});
		return user;
	}

	public String getUserUuid() {
		return userUuid;
	}

	public String getPassword() {
		return password;
	}

}