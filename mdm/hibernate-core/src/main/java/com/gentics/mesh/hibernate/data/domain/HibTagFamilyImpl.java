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
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
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
public class HibTagFamilyImpl extends AbstractHibUserTrackedElement<TagFamilyResponse> implements TagFamily, Serializable {

	private static final long serialVersionUID = 8600788302248772724L;

	@OneToMany(targetEntity = HibTagImpl.class, mappedBy = "tagFamily")
	private Set<Tag> tags = new HashSet<>();

	@ManyToOne(targetEntity = HibProjectImpl.class)
	private Project project;

	private String description;

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setProject(Project project) {
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

	public Set<Tag> getTags() {
		return tags;
	}

	@Override
	public Result<? extends Tag> findAllTags() {
		return new TraversalResult<>(Collections.unmodifiableSet(tags));
	}

	@Override
	public void addTag(Tag tag) {
		tags.add(tag);
		tag.setTagFamily(this);
	}

	@Override
	public void removeTag(Tag tag) {
		tags.remove(tag);
		tag.setTagFamily(null);
	}
}
