package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Project entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "project")
@ElementTypeKey(ElementType.PROJECT)
public class HibProjectImpl extends AbstractHibUserTrackedElement<ProjectResponse> implements HibProject, Serializable {

	private static final long serialVersionUID = 10348507855724965L;

	@ManyToMany(targetEntity = HibLanguageImpl.class, fetch = FetchType.LAZY)
	private Set<HibLanguage> languages = new HashSet<>();

	@OneToMany(mappedBy = "project", targetEntity = HibBranchImpl.class, fetch = FetchType.LAZY)
	private Set<HibBranch> branches = new HashSet<>();

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@ManyToMany(targetEntity = HibSchemaImpl.class, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private Set<HibSchema> schemas = new HashSet<>();

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@ManyToMany(targetEntity = HibMicroschemaImpl.class, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private Set<HibMicroschema> microschemas = new HashSet<>();

	@OneToOne(targetEntity = HibNodeImpl.class, fetch = FetchType.LAZY)
	private HibNode baseNode;

	@OneToOne
	private HibBranchImpl latestBranch;

	@OneToOne
	private HibBranchImpl initialBranch;

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<HibPermissionRootImpl> roots = new HashSet<>();

	@OneToMany(mappedBy = "project", targetEntity = HibTagFamilyImpl.class)
	private Set<HibTagFamily> tagFamilies = new HashSet<>();

	public Set<HibBranch> getBranches() {
		return branches;
	}

	@Override
	public Result<? extends HibSchema> getSchemas() {
		return new TraversalResult<>(schemas);
	}

	@Override
	public Result<? extends HibMicroschema> getMicroschemas() {
		return new TraversalResult<>(microschemas);
	}

	public Set<HibSchema> getHibSchemas() {
		return schemas;
	}

	public Set<HibMicroschema> getHibMicroschemas() {
		return microschemas;
	}

	public Set<HibBranch> getHibBranches() {
		return branches;
	}

	@Override
	public HibBranch getLatestBranch() {
		return latestBranch;
	}

	@Override
	public HibBranch getInitialBranch() {
		return initialBranch;
	}

	public HibProjectImpl setInitialBranch(HibBranchImpl initialBranch) {
		this.initialBranch = initialBranch;
		return this;
	}

	public void setLatestBranch(HibBranchImpl branch) {
		this.latestBranch = branch;
	}

	@Override
	public HibBaseElement getBranchPermissionRoot() {
		return CommonTx.get().projectDao().getBranchPermissionRoot(this);
	}

	@Override
	public HibBaseElement getTagFamilyPermissionRoot() {
		return CommonTx.get().projectDao().getTagFamilyPermissionRoot(this);
	}

	@Override
	public HibNode getBaseNode() {
		return baseNode;
	}

	@Override
	public void setBaseNode(HibNode baseNode) {
		this.baseNode = baseNode;
	}

	@Override
	public HibBaseElement getSchemaPermissionRoot() {
		return  CommonTx.get().projectDao().getSchemaContainerPermissionRoot(this);
	}

	@Override
	public HibBaseElement getNodePermissionRoot() {
		return CommonTx.get().projectDao().getNodePermissionRoot(this);
	}

	@Override
	public Result<? extends HibLanguage> getLanguages() {
		return new TraversalResult<>(getHibLanguages());
	}

	@Override
	public void removeLanguage(HibLanguage language) {
		getHibLanguages().remove(language);
	}

	@Override
	public void addLanguage(HibLanguage language) {
		getHibLanguages().add(language);
	}

	public Set<HibLanguage> getHibLanguages() {
		return languages;
	}

	public void addBranch(HibBranch branch) {
		branches.add(branch);
		branch.setProject(this);
	}

	public void removeBranch(HibBranch branch) {
		branch.setProject(null);
		branches.remove(branch);
	}

	public Set<HibTagFamily> getTagFamilies() {
		return tagFamilies;
	}

	public void addTagFamily(HibTagFamily tagFamily) {
		tagFamilies.add(tagFamily);
		tagFamily.setProject(this);
	}

	public void removeTagFamily(HibTagFamily tagFamily) {
		tagFamilies.remove(tagFamily);
		tagFamily.setProject(null);
	}

	public void addPermissionRoot(HibPermissionRootImpl permissionRoot) {
		roots.add(permissionRoot);
	}

	@Override
	public HibLanguage findLanguageByTag(String languageTag) {
		return Optional.ofNullable(languages).stream().flatMap(Set::stream).filter(l -> l.getLanguageTag().equals(languageTag)).findAny().orElse(null);
	}
}
