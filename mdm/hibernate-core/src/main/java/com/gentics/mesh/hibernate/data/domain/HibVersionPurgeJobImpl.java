package com.gentics.mesh.hibernate.data.domain;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.core.data.job.HibVersionPurgeJob;
import com.gentics.mesh.core.data.job.JobCore;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.util.DateUtils;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;

/**
 * Version purge job entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Entity
public class HibVersionPurgeJobImpl extends HibJobImpl implements JobCore, HibVersionPurgeJob {

	private static final long serialVersionUID = 3993783122932333210L;

	@OneToOne(targetEntity = HibProjectImpl.class)
	private HibProject project;

	private Instant maxAge;

	@Override
	public HibProject getProject() {
		return project;
	}

	@Override
	public void setProject(HibProject project) {
		this.project = project;
	}

	@Override
	public Optional<ZonedDateTime> getMaxAge() {
		return Optional.ofNullable(maxAge).map(age -> DateUtils.toZonedDateTime(age.toEpochMilli()));
	}

	@Override
	public void setMaxAge(ZonedDateTime maxAge) {
		this.maxAge = Optional.ofNullable(maxAge).map(ZonedDateTime::toInstant).orElse(null);
	}
}
