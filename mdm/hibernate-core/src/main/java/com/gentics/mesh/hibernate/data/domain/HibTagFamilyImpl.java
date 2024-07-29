package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Tag family entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "tagfamily")
@ElementTypeKey(ElementType.TAGFAMILY)
public class HibTagFamilyImpl extends AbstractHibUserTrackedElement<TagFamilyResponse> implements HibTagFamily, Serializable {

	private static final long serialVersionUID = 8600788302248772724L;

	@OneToMany(targetEntity = HibTagImpl.class, mappedBy = "tagFamily")
	private Set<HibTag> tags = new HashSet<>();

	@ManyToOne(targetEntity = HibProjectImpl.class)
	private HibProject project;

	private String description;

	@Override
	public HibProject getProject() {
		return project;
	}

	@Override
	public void setProject(HibProject project) {
		this.project = project;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	public Set<HibTag> getTags() {
		return tags;
	}

	@Override
	public Result<? extends HibTag> findAllTags() {
		return new TraversalResult<>(Collections.unmodifiableSet(tags));
	}

	@Override
	public void addTag(HibTag tag) {
		tags.add(tag);
		tag.setTagFamily(this);
	}

	@Override
	public void removeTag(HibTag tag) {
		tags.remove(tag);
		tag.setTagFamily(null);
	}
}
