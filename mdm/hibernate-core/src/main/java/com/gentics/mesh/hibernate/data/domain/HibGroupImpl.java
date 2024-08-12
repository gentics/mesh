package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.util.HibernateUtil;

/**
 * Group entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@NamedEntityGraph(
	name = "group.rest",
	attributeNodes = {
		@NamedAttributeNode("roles")
	}
)
@NamedQueries({
	@NamedQuery(
			name = "group.findrolesforgroups",
			query = "select g, r from group g inner join g.roles r where g.dbUuid in :groupUuids")
})
@Entity(name = "group")
@ElementTypeKey(ElementType.GROUP)
public class HibGroupImpl extends AbstractHibUserTrackedElement<GroupResponse> implements HibGroup, Serializable {

	private static final long serialVersionUID = -4625309053762578939L;

	@ManyToMany(targetEntity = HibUserImpl.class, fetch = FetchType.LAZY)
	@JoinTable(name = "group_user", inverseJoinColumns = {@JoinColumn(name = "users_dbUuid")}, joinColumns = {@JoinColumn(name = "groups_dbUuid")})
	private Set<HibUser> users = new HashSet<>();

	@ManyToMany(targetEntity = HibRoleImpl.class, fetch = FetchType.LAZY)
	@JoinTable(name = "group_role", inverseJoinColumns = {@JoinColumn(name = "roles_dbUuid")}, joinColumns = {@JoinColumn(name = "groups_dbUuid")})
	private Set<HibRole> roles = new HashSet<>();

	@Override
	public GroupReference transformToReference() {
		return new GroupReference()
			.setName(getName())
			.setUuid(getUuid());
	}

	@Override
	public MeshElementEventModel onCreated() {
		return HibernateTx.get().groupDao().onCreated(this);
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return HibernateTx.get().groupDao().onUpdated(this);
	}

	@Override
	public MeshElementEventModel onDeleted() {
		return HibernateTx.get().groupDao().onDeleted(this);
	}

	public Stream<HibUser> getUsers() {
		return users.stream();
	}

	public void addUser(HibUser user) {
		getUsers().filter(u -> u.getId().equals(user.getId())).findAny().ifPresentOrElse(u -> {}, () -> {
			users.add(user);
		});
	}

	public void removeUser(HibUser user) {
		HibernateUtil.dropGroupUserConnection(HibernateTx.get().entityManager(), user, this);
	}

	public void addRole(HibRole role) {
		getRoles().filter(r -> r.getId().equals(role.getId())).findAny().ifPresentOrElse(u -> {}, () -> {
			roles.add(role);
		});
	}

	public void removeRole(HibRole role) {
		HibernateUtil.dropGroupRoleConnection(HibernateTx.get().entityManager(), role, this);
	}

	public Stream<HibRole> getRoles() {
		return roles.stream();
	}

	public void removeRoles() {
		GroupDao groupDao = Tx.get().groupDao();
		roles.forEach(role -> groupDao.removeRole(this, role));
		roles.clear();
	}

	public void removeUsers() {
		GroupDao groupDao = Tx.get().groupDao();
		users.forEach(user -> groupDao.removeUser(this, user));
		users.clear();
	}
}
