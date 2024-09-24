package com.gentics.mesh.hibernate.data.domain;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Tag entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "tag")
@ElementTypeKey(ElementType.TAG)
public class HibTagImpl extends AbstractHibUserTrackedElement<TagResponse> implements HibTag, Serializable {

	private static final long serialVersionUID = -3121676186452994502L;

	@ManyToOne(targetEntity = HibTagFamilyImpl.class)
	private HibTagFamily tagFamily;

	@ManyToOne(targetEntity = HibProjectImpl.class)
	private HibProject project;

	@Override
	public HibTagFamily getTagFamily() {
		return tagFamily;
	}

	@Override
	public void setTagFamily(HibTagFamily tagFamily) {
		this.tagFamily = tagFamily;
	}

	@Override
	public HibProject getProject() {
		return project;
	}

	@Override
	public void setProject(HibProject project) {
		this.project = project;
	}
}
