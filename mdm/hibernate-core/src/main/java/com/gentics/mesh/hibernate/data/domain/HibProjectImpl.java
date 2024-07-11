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
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Project entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "project")
@ElementTypeKey(ElementType.PROJECT)
public class HibProjectImpl extends AbstractHibUserTrackedElement<ProjectResponse> implements Project, Serializable {

	private static final long serialVersionUID = 10348507855724965L;

	@ManyToMany(targetEntity = HibLanguageImpl.class, fetch = FetchType.LAZY)
	private Set<Language> languages = new HashSet<>();

	@OneToMany(mappedBy = "project", targetEntity = HibBranchImpl.class, fetch = FetchType.LAZY)
	private Set<Branch> branches = new HashSet<>();

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@ManyToMany(targetEntity = HibSchemaImpl.class, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private Set<Schema> schemas = new HashSet<>();

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@ManyToMany(targetEntity = HibMicroschemaImpl.class, fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
	private Set<Microschema> microschemas = new HashSet<>();

	@OneToOne(targetEntity = HibNodeImpl.class, fetch = FetchType.LAZY)
	private Node baseNode;

	@OneToOne
	private HibBranchImpl latestBranch;

	@OneToOne
	private HibBranchImpl initialBranch;

	@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<HibPermissionRootImpl> roots = new HashSet<>();

	@OneToMany(mappedBy = "project", targetEntity = HibTagFamilyImpl.class)
	private Set<TagFamily> tagFamilies = new HashSet<>();

	public Set<Branch> getBranches() {
		return branches;
	}

	@Override
	public Result<? extends Schema> getSchemas() {
		return new TraversalResult<>(schemas);
	}

	@Override
	public Result<? extends Microschema> getMicroschemas() {
		return new TraversalResult<>(microschemas);
	}

	public Set<Schema> getHibSchemas() {
		return schemas;
	}

	public Set<Microschema> getHibMicroschemas() {
		return microschemas;
	}

	public Set<Branch> getHibBranches() {
		return branches;
	}

	@Override
	public Branch getLatestBranch() {
		return latestBranch;
	}

	@Override
	public Branch getInitialBranch() {
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
	public BaseElement getBranchPermissionRoot() {
		return CommonTx.get().projectDao().getBranchPermissionRoot(this);
	}

	@Override
	public BaseElement getTagFamilyPermissionRoot() {
		return CommonTx.get().projectDao().getTagFamilyPermissionRoot(this);
	}

	@Override
	public Node getBaseNode() {
		return baseNode;
	}

	@Override
	public void setBaseNode(Node baseNode) {
		this.baseNode = baseNode;
	}

	@Override
	public BaseElement getSchemaPermissionRoot() {
		return  CommonTx.get().projectDao().getSchemaContainerPermissionRoot(this);
	}

	@Override
	public BaseElement getNodePermissionRoot() {
		return CommonTx.get().projectDao().getNodePermissionRoot(this);
	}

	@Override
	public Result<? extends Language> getLanguages() {
		return new TraversalResult<>(getHibLanguages());
	}

	@Override
	public void removeLanguage(Language language) {
		getHibLanguages().remove(language);
	}

	@Override
	public void addLanguage(Language language) {
		getHibLanguages().add(language);
	}

	public Set<Language> getHibLanguages() {
		return languages;
	}

	public void addBranch(Branch branch) {
		branches.add(branch);
		branch.setProject(this);
	}

	public void removeBranch(Branch branch) {
		branch.setProject(null);
		branches.remove(branch);
	}

	public Set<TagFamily> getTagFamilies() {
		return tagFamilies;
	}

	public void addTagFamily(TagFamily tagFamily) {
		tagFamilies.add(tagFamily);
		tagFamily.setProject(this);
	}

	public void removeTagFamily(TagFamily tagFamily) {
		tagFamilies.remove(tagFamily);
		tagFamily.setProject(null);
	}

	public void addPermissionRoot(HibPermissionRootImpl permissionRoot) {
		roots.add(permissionRoot);
	}

	@Override
	public Language findLanguageByTag(String languageTag) {
		return Optional.ofNullable(languages).stream().flatMap(Set::stream).filter(l -> l.getLanguageTag().equals(languageTag)).findAny().orElse(null);
	}
}
