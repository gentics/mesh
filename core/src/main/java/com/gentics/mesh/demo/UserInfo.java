package com.gentics.mesh.demo;

import java.util.function.Consumer;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;

/**
 * Container for user, group, role and password references of an user.
 */
public class UserInfo {

	private User user;
	private String userUuid;

	private Group group;
	private String groupUuid;

	private Role role;
	private String roleUuid;

	private String password;

	public UserInfo(User user, Group group, Role role, String password) {
		this.user = user;
		this.userUuid = user.getUuid();
		this.group = group;
		this.groupUuid = group.getUuid();
		this.role = role;
		this.roleUuid = role.getUuid();
		this.password = password;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

	public Group getGroup() {
		return getBaseElement(() -> group, a -> group = a);
	}

	public String getGroupUuid() {
		return groupUuid;
	}

	public Role getRole() {
		return getBaseElement(() -> role, a -> role = a);
	}

	public String getRoleUuid() {
		return roleUuid;
	}

	public User getUser() {
		return getBaseElement(() -> user, a -> user = a);
	}

	@SuppressWarnings("unchecked")
	private <T extends BaseElement> T getBaseElement(Supplier<T> getter, Consumer<T> setter) {
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