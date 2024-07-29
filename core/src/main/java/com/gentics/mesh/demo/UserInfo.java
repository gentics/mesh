package com.gentics.mesh.demo;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.HibBaseElement;
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
		return getBaseElement(() -> group, a -> group = a);
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public HibRole getRole() {
		return getBaseElement(() -> role, a -> role = a);
	}

	public String getRoleUuid() {
		return roleUuid;
	}

	public HibUser getUser() {
		return getBaseElement(() -> user, a -> user = a);
	}

	@SuppressWarnings("unchecked")
	private <T extends HibBaseElement> T getBaseElement(Supplier<T> getter, Consumer<T> setter) {
		Tx.maybeGet().ifPresent(tx -> {
			T original = getter.get();
			CommonTx ctx = tx.unwrap();
			setter.accept((T) ctx.load(original.getId(), (Class<T>) ctx.entityClassOf(original)));
		});
		return getter.get();
	}

	public String getUserUuid() {
		return userUuid;
	}

	public String getPassword() {
		return password;
	}
}