package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.RoleDaoImpl;
import com.gentics.mesh.hibernate.util.HibernateUtil;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.NamedSubgraph;
import jakarta.persistence.OneToMany;

/**
 * Role entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@NamedEntityGraph(
	name = "role.rest",
	attributeNodes = {
		@NamedAttributeNode(value = "groups", subgraph = "groups-subgraph")
	},
	subgraphs = {
		@NamedSubgraph(
			name = "groups-subgraph",
			attributeNodes = {
				@NamedAttributeNode(value = "name")
			}
		)
	}
)
@NamedQueries({
	@NamedQuery(
		name = "role.findAllForAdmin",
		query = "select dbUuid as uuid, name as name from role"
	),
	@NamedQuery(
		name = "role.findAll",
		query = "select r.dbUuid as uuid, r.name as name from role r" + 
				" join permission perm on (perm.element = r.dbUuid) " +
				" where perm.role in :roles " +
				" and perm.readPerm = true"
	)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "role")
@ElementTypeKey(ElementType.ROLE)
public class HibRoleImpl extends AbstractHibUserTrackedElement<RoleResponse> implements HibRole, Serializable {

	private static final long serialVersionUID = -5790764419841421849L;

	@ManyToMany(targetEntity = HibGroupImpl.class, fetch = FetchType.LAZY)
	@JoinTable(name = "group_role", joinColumns = {@JoinColumn(name = "roles_dbUuid")}, inverseJoinColumns = {@JoinColumn(name = "groups_dbUuid")})
	private Set<HibGroup> groups = new HashSet<>();

	@OneToMany(mappedBy = "role", cascade = CascadeType.REMOVE)
	private Set<HibPermissionImpl> permissions = new HashSet<>();

	@Override
	public RoleReference transformToReference() {
		return new RoleReference()
			.setName(getName())
			.setUuid(getUuid());
	}

	@Override
	public void removeElement() {
		Tx.get().roleDao().removeRole(this);
	}

	@Override
	public String getElementVersion() {
		return Long.toString(getDbVersion());
	}

	@Override
	public MeshElementEventModel onCreated() {
		return ((RoleDaoImpl) Tx.get().roleDao()).onCreated(this);
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return ((RoleDaoImpl) Tx.get().roleDao()).onUpdated(this);
	}

	@Override
	public MeshElementEventModel onDeleted() {
		return ((RoleDaoImpl) Tx.get().roleDao()).onDeleted(this);
	}

	@Override
	public Result<? extends HibGroup> getGroups() {
		return new TraversalResult<>(groups.iterator());
	}

	public void removeGroups() {
		GroupDao groupDao = Tx.get().groupDao();
		groups.forEach(group -> groupDao.removeRole(group, this));
		groups.clear();
	}

	public void removeGroup(HibGroupImpl hibGroup) {
		HibernateUtil.dropGroupRoleConnection(HibernateTx.get().entityManager(), this, hibGroup);
	}

	public void addGroup(HibGroup group) {
		groups.stream().filter(g -> g.getId().equals(group.getId())).findAny().ifPresentOrElse(u -> {}, () -> {
			groups.add(group);
		});
	}
}
