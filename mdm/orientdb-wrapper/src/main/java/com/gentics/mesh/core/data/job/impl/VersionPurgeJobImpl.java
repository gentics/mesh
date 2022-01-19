package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.job.HibVersionPurgeJob;
import com.gentics.mesh.core.data.job.JobCore;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.util.DateUtils;

/**
 * Graph entity for version purge jobs.
 */
public class VersionPurgeJobImpl extends JobImpl implements JobCore, HibVersionPurgeJob {

	private static final String MAX_AGE_PROPERTY = "maxAge";

	/**
	 * Initialize the vertex type and index.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(VersionPurgeJobImpl.class, MeshVertexImpl.class);
	}

	public Project getProject() {
		return out(HAS_PROJECT, ProjectImpl.class).nextOrNull();
	}

	/**
	 * Set the project reference for the job.
	 * 
	 * @param project
	 */
	public void setProject(HibProject project) {
		setSingleLinkOutTo(toGraph(project), HAS_PROJECT);
	}

	/**
	 * Return the max age setting for the purge operation.
	 * 
	 * @return
	 */
	public Optional<ZonedDateTime> getMaxAge() {
		Long maxAge = getProperty(MAX_AGE_PROPERTY);
		return Optional.ofNullable(maxAge).map(DateUtils::toZonedDateTime);
	}

	/***
	 * Set the max age setting for the purge operation.
	 * 
	 * @param time
	 */
	public void setMaxAge(ZonedDateTime time) {
		if (time != null) {
			Long ms = time.toInstant().toEpochMilli();
			setProperty(MAX_AGE_PROPERTY, ms);
		} else {
			removeProperty(MAX_AGE_PROPERTY);
		}
	}
}
