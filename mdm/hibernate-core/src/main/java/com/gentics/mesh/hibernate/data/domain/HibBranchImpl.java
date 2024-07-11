package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.branch.BranchMicroschemaVersion;
import com.gentics.mesh.core.data.branch.BranchSchemaVersion;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.FieldSchemaElement;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.VersionUtil;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Branch entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "branch")
@ElementTypeKey(ElementType.BRANCH)
@NamedQueries({
		@NamedQuery(
				name = "branch.findFromProject",
				query = "select b from branch b where b.project = :project"
		),
		@NamedQuery(
				name = "branch.findForTag",
				query = "select b from branch b join b.tags t where t = :tag"
		)
})
public class HibBranchImpl extends AbstractHibUserTrackedElement<BranchResponse> implements Branch, Serializable {

	private static final long serialVersionUID = -5133700117945091432L;

	@ManyToOne(targetEntity = HibProjectImpl.class)
	private Project project;

	@OneToOne(fetch = FetchType.LAZY, targetEntity = HibBranchImpl.class)
	private Branch previousBranch;

	@OneToMany(mappedBy = "branch", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<HibBranchSchemaVersionEdgeImpl> schemaVersions = new HashSet<>();

	@OneToMany(mappedBy = "branch", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<HibBranchMicroschemaVersionEdgeImpl> microschemaVersions = new HashSet<>();

	@ManyToMany(targetEntity = HibTagImpl.class, fetch = FetchType.LAZY)
	private Set<Tag> tags = new HashSet<>();

	private String hostName;

	private String pathPrefix;

	private boolean isSsl;

	private boolean isActive;

	private boolean isMigrated;

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public Branch setActive(boolean active) {
		this.isActive = active;
		return this;
	}

	@Override
	public boolean isMigrated() {
		return isMigrated;
	}

	@Override
	public Branch setMigrated(boolean migrated) {
		this.isMigrated = migrated;
		return this;
	}

	@Override
	public String getHostname() {
		return hostName;
	}

	@Override
	public Branch setHostname(String hostname) {
		this.hostName = hostname;
		return this;
	}

	@Override
	public Boolean getSsl() {
		return isSsl;
	}

	@Override
	public Branch setSsl(boolean ssl) {
		this.isSsl = ssl;
		return this;
	}

	@Override
	public String getPathPrefix() {
		return pathPrefix;
	}

	@Override
	public Branch setPathPrefix(String pathPrefix) {
		this.pathPrefix = pathPrefix;
		return this;
	}

	public HibBranchSchemaVersionEdgeImpl addSchemaVersion(HibSchemaVersionImpl version) {
		HibernateTx tx = HibernateTx.get();
		HibBranchSchemaVersionEdgeImpl sve = new HibBranchSchemaVersionEdgeImpl(tx, this, version);
		tx.entityManager().persist(sve);
		schemaVersions.add(sve);
		return sve;
	}

	public HibBranchMicroschemaVersionEdgeImpl addMicroschemaVersion(HibMicroschemaVersionImpl version) {
		HibernateTx tx = HibernateTx.get();
		HibBranchMicroschemaVersionEdgeImpl mve = new HibBranchMicroschemaVersionEdgeImpl(tx, this, version);
		tx.entityManager().persist(mve);
		microschemaVersions.add(mve);
		return mve;
	}

	public boolean removeSchemaVersion(SchemaVersion version) {
		return removeVersion(version, schemaVersions);
	}

	public boolean removeMicroschemaVersion(MicroschemaVersion version) {
		return removeVersion(version, microschemaVersions);
	}

	@Override
	public boolean isLatest() {
		return this.equals(project.getLatestBranch());
	}

	@Override
	public Branch setLatest() {
		((HibProjectImpl)project).setLatestBranch(this);
		return this;
	}

	@Override
	public Branch setInitial() {
		((HibProjectImpl) project).setInitialBranch(this);
		return this;
	}

	@Override
	public List<? extends Branch> getNextBranches() {
		EntityManager em = HibernateTx.get().entityManager();
		return em.createQuery("select b from branch b where b.previousBranch = :thisBranch", HibBranchImpl.class)
				.setParameter("thisBranch", this)
				.getResultList();
	}

	@Override
	public Branch setPreviousBranch(Branch branch) {
		previousBranch = branch;
		return this;
	}

	@Override
	public Branch getPreviousBranch() {
		return previousBranch;
	}

	@Override
	public Branch unassignSchema(Schema schemaContainer) {
		schemaVersions.removeIf(sve -> sve.getSchemaContainerVersion().getSchemaContainer().getId().equals(schemaContainer.getId()));
		return this;
	}

	@Override
	public boolean contains(Schema schema) {
		return schemaVersions.stream().anyMatch(sve -> sve.getVersion().getSchemaContainer().getUuid().equals(schema.getUuid()));
	}

	@Override
	public boolean contains(SchemaVersion version) {
		return schemaVersions.stream().anyMatch(sve -> sve.getVersion().getUuid().equals(version.getUuid()));
	}

	@Override
	public TraversalResult<? extends SchemaVersion> findAllSchemaVersions() {
		return new TraversalResult<>(schemaVersions.stream().map(HibBranchSchemaVersionEdgeImpl::getVersion).iterator());
	}

	@Override
	public Branch unassignMicroschema(Microschema microschema) {
		microschemaVersions.removeIf(mve -> mve.getMicroschemaContainerVersion().getSchemaContainer().getId().equals(microschema.getId()));
		return this;
	}

	@Override
	public boolean contains(Microschema microschema) {
		return microschemaVersions.stream().anyMatch(mve -> mve.getVersion().getSchemaContainer().getUuid().equals(microschema.getUuid()));
	}

	@Override
	public boolean contains(MicroschemaVersion microschemaVersion) {
		return microschemaVersions.stream().anyMatch(mve -> mve.getVersion().getUuid().equals(microschemaVersion.getUuid()));
	}

	@Override
	public TraversalResult<? extends MicroschemaVersion> findAllMicroschemaVersions() {
		return new TraversalResult<>(microschemaVersions.stream().map(HibBranchMicroschemaVersionEdgeImpl::getVersion).iterator());
	}

	@Override
	public TraversalResult<? extends BranchMicroschemaVersion> findAllLatestMicroschemaVersionEdges() {
		Collection<HibBranchMicroschemaVersionEdgeImpl> latestSchemaVersionEdges = microschemaVersions.stream()
				.collect(Collectors.toMap(sve -> sve.getVersion().getSchemaContainer(),
						Function.identity(),
						(a, b) -> a.getMicroschemaContainerVersion().compareTo(b.getMicroschemaContainerVersion()) > 0 ? a : b))
				.values();

		return new TraversalResult<>(latestSchemaVersionEdges.iterator());
	}

	@Override
	public TraversalResult<? extends SchemaVersion> findActiveSchemaVersions() {
		return new TraversalResult<>(schemaVersions.stream().filter(HibBranchSchemaVersionEdgeImpl::isActive).map(HibBranchSchemaVersionEdgeImpl::getVersion));
	}

	@Override
	public Iterable<? extends MicroschemaVersion> findActiveMicroschemaVersions() {
		return new TraversalResult<>(microschemaVersions.stream().filter(HibBranchMicroschemaVersionEdgeImpl::isActive).map(HibBranchMicroschemaVersionEdgeImpl::getVersion));
	}

	@Override
	public Iterable<? extends BranchSchemaVersion> findAllLatestSchemaVersionEdges() {
		Collection<HibBranchSchemaVersionEdgeImpl> latestSchemaVersionEdges = schemaVersions.stream()
				.collect(Collectors.toMap(sve -> sve.getVersion().getSchemaContainer(),
						Function.identity(),
						(a, b) -> a.getSchemaContainerVersion().compareTo(b.getSchemaContainerVersion()) > 0 ? a : b))
				.values();

		return new TraversalResult<>(latestSchemaVersionEdges.iterator());
	}

	@Override
	public Branch setProject(Project project) {
		this.project = project;
		return this;
	}

	@Override
	public TraversalResult<? extends BranchSchemaVersion> findAllSchemaVersionEdges() {
		return new TraversalResult<>(schemaVersions);
	}

	@Override
	public TraversalResult<? extends BranchMicroschemaVersion> findAllMicroschemaVersionEdges() {
		return new TraversalResult<>(microschemaVersions);
	}

	@Override
	public SchemaVersion findLatestSchemaVersion(Schema schemaContainer) {
		return schemaVersions.stream()
				.filter(v -> schemaContainer.getUuid().equals(v.getVersion().getSchemaContainer().getUuid()))
				.sorted((v1, v2) -> VersionUtil.compareVersions(v2.getVersion().getVersion(), v1.getVersion().getVersion()))
				.map(AbstractHibBranchSchemaVersion::getVersion)
				.findFirst()
				.orElse(null);
	}

	@Override
	public MicroschemaVersion findLatestMicroschemaVersion(Microschema microschemaContainer) {
		return microschemaVersions.stream()
				.filter(v -> microschemaContainer.getUuid().equals(v.getVersion().getSchemaContainer().getUuid()))
				.sorted((v1, v2) -> VersionUtil.compareVersions(v2.getVersion().getVersion(), v1.getVersion().getVersion()))
				.map(AbstractHibBranchSchemaVersion::getVersion)
				.findFirst()
				.orElse(null);
	}

	@Override
	public void addTag(Tag tag) {
		tags.add(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		tags.remove(tag);
	}

	@Override
	public void removeAllTags() {
		tags.clear();
	}

	@Override
	public TraversalResult<? extends Tag> getTags() {
		return new TraversalResult<>(tags.iterator());
	}

	@Override
	public Page<? extends Tag> getTags(User user, PagingParameters params) {
		UserDao userDao = Tx.get().userDao();
		return new DynamicStreamPageImpl<>(tags.stream(), params, ( t) -> userDao.hasPermission(user, t, InternalPermission.READ_PERM), false);
	}

	@Override
	public boolean hasTag(Tag tag) {
		return tags.contains(tag);
	}

	@Override
	public Tag findTagByUuid(String uuid) {
		return tags.stream().filter(t -> t.getUuid().equals(uuid)).findFirst().orElse(null);
	}

	@Override
	public BranchReference transformToReference() {
		return new BranchReference().setName(getName()).setUuid(getUuid());
	}

	private static final <
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends FieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends FieldSchemaVersionElement<R, RM, RE, SC, SCV>, 
			I extends AbstractHibFieldSchemaVersion<R, RM, RE, SC, SCV>
	> boolean removeVersion(SCV version, Set<? extends AbstractHibBranchSchemaVersion<I>> versions) {
		HibernateTx tx = HibernateTx.get();

		// Collect versions to delete
		List<? extends AbstractHibBranchSchemaVersion<I>> toRemove = versions.stream()
				.filter(mve -> mve.getVersion().getId().equals(version.getId()))
				.collect(Collectors.toList());
		// Delete versions from branch and 
		toRemove.stream()
				.forEach(edge -> {
					versions.remove(edge);
					tx.entityManager().remove(edge);
				});
		return !toRemove.isEmpty();
	}
}
