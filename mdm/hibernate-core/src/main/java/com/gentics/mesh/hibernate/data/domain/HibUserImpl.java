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
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.dao.UserDaoImpl;
import com.gentics.mesh.hibernate.util.HibernateUtil;

/**
 * User entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "user")
@ElementTypeKey(ElementType.USER)
public class HibUserImpl extends AbstractHibUserTrackedElement<UserResponse> implements HibUser, Serializable {

	private static final long serialVersionUID = -8445998784074594208L;

	@ManyToMany(targetEntity = HibGroupImpl.class, fetch = FetchType.LAZY)
	@JoinTable(name = "group_user", inverseJoinColumns = {@JoinColumn(name = "groups_dbUuid")}, joinColumns = {@JoinColumn(name = "users_dbUuid")})
	private Set<HibGroup> groups = new HashSet<>();
	
	@OneToOne(targetEntity = HibNodeImpl.class, fetch = FetchType.LAZY)
	private HibNode referencedNode;

	private String firstname;

	private String lastname;

	private String emailAddress;

	private String passwordHash;
	
	private String resetToken;
	
	private Long resetTokenIssueTimestamp;
	
	private String apiTokenId;
	
	private Long apiTokenIssueTimestamp;

	private boolean forcedPasswordChange;

	private boolean enabled;

	private boolean admin;

	@Override
	public String getUsername() {
		return getName();
	}

	@Override
	public HibUserImpl setUsername(String username) {
		setName(username);
		return this;
	}

	@Override
	public String getLastname() {
		return lastname;
	}

	@Override
	public HibUser setLastname(String lastname) {
		this.lastname = lastname;
		return this;
	}

	@Override
	public String getFirstname() {
		return firstname;
	}

	@Override
	public HibUserImpl setFirstname(String firstname) {
		this.firstname = firstname;
		return this;
	}

	@Override
	public String getEmailAddress() {
		return emailAddress;
	}

	@Override
	public HibUserImpl setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
		return this;
	}

	@Override
	public String getPasswordHash() {
		return passwordHash;
	}

	@Override
	public HibUser disable() {
		this.enabled = false;
		return this;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}

	@Override
	public HibUser enable() {
		this.enabled = true;
		return this;
	}

	public Stream<HibGroup> getGroups() {
		return groups.stream();
	}

	@Override
	public UserReference transformToReference() {
		return new UserReference()
			.setFirstName(firstname)
			.setLastName(lastname)
			.setUuid(getUuid());
	}

	@Override
	public Long getResetTokenIssueTimestamp() {
		return resetTokenIssueTimestamp;
	}

	@Override
	public HibUser invalidateResetToken() {
		setResetToken(null);
		setResetTokenIssueTimestamp(null);
		return this;
	}

	@Override
	public HibUser setPasswordHash(String hash) {
		this.passwordHash = hash;
		return this;
	}

	@Override
	public boolean isForcedPasswordChange() {
		return forcedPasswordChange;
	}

	@Override
	public HibUser setForcedPasswordChange(boolean force) {
		this.forcedPasswordChange = force;
		return this;
	}

	@Override
	public boolean isAdmin() {
		return admin;
	}

	@Override
	public void setAdmin(boolean admin) {
		this.admin = admin;
	}

	@Override
	public String getAPIKeyTokenCode() {
		return apiTokenId;
	}

	@Override
	public Long getAPITokenIssueTimestamp() {
		return apiTokenIssueTimestamp;
	}

	@Override
	public void resetAPIToken() {
		apiTokenId = null;
		apiTokenIssueTimestamp = null;
	}

	@Override
	public HibUser setResetToken(String token) {
		resetToken = token;
		return this;
	}

	@Override
	public HibUser setAPITokenId(String code) {
		apiTokenId = code;
		return this;
	}

	@Override
	public HibUser setResetTokenIssueTimestamp(Long timestamp) {
		resetTokenIssueTimestamp = timestamp;
		return this;
	}

	@Override
	public HibUser setAPITokenIssueTimestamp() {
		apiTokenIssueTimestamp = System.currentTimeMillis();
		return this;
	}

	@Override
	public String getResetToken() {
		return resetToken;
	}

	@Override
	public HibNode getReferencedNode() {
		return referencedNode;
	}

	@Override
	public HibUser setReferencedNode(HibNode node) {
		referencedNode = node;
		return this;
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return Tx.get().userDao().findMeshAuthUserByUuid(getUuid());
	}

	@Override
	public MeshElementEventModel onCreated() {
		return ((UserDaoImpl) Tx.get().userDao()).onCreated(this);
	}

	@Override
	public MeshElementEventModel onUpdated() {
		return ((UserDaoImpl) Tx.get().userDao()).onUpdated(this);
	}

	@Override
	public MeshElementEventModel onDeleted() {
		return ((UserDaoImpl) Tx.get().userDao()).onDeleted(this);
	}

	public void removeGroup(HibGroupImpl hibGroup) {
		HibernateUtil.dropGroupUserConnection(HibernateTx.get().entityManager(), this, hibGroup);
	}

	public void removeGroups() {
		GroupDao groupDao = Tx.get().groupDao();
		groups.forEach(group -> groupDao.removeUser(group, this));
		groups.clear();
	}

	public void addGroup(HibGroup group) {
		getGroups().filter(g -> g.getId().equals(group.getId())).findAny().ifPresentOrElse(u -> {}, () -> {
			groups.add(group);
		});
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		return HibUser.super.getSubETag(ac);
	}
}
