package com.gentics.mesh.hibernate.data.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.tagfamily.TagFamily;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Tag entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "tag")
@ElementTypeKey(ElementType.TAG)
public class HibTagImpl extends AbstractHibUserTrackedElement<TagResponse> implements Tag, Serializable {

	private static final long serialVersionUID = -3121676186452994502L;

	@ManyToOne(targetEntity = HibTagFamilyImpl.class)
	private TagFamily tagFamily;

	@ManyToOne(targetEntity = HibProjectImpl.class)
	private Project project;

	@Override
	public TagFamily getTagFamily() {
		return tagFamily;
	}

	@Override
	public void setTagFamily(TagFamily tagFamily) {
		this.tagFamily = tagFamily;
	}

	@Override
	public Project getProject() {
		return project;
	}

	@Override
	public void setProject(Project project) {
		this.project = project;
	}
}
