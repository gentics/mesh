package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Container schema entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "schema")
@ElementTypeKey(ElementType.SCHEMA)
public class HibSchemaImpl extends AbstractHibUserTrackedElement<SchemaResponse> implements Schema, Serializable {

	private static final long serialVersionUID = 8334546969092825031L;

	@ManyToMany(mappedBy = "schemas", targetEntity = HibProjectImpl.class, fetch = FetchType.LAZY)
	private Set<Project> projects = new HashSet<>();

	@OneToOne(targetEntity = HibSchemaVersionImpl.class)
	private SchemaVersion latestVersion;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@OneToMany(mappedBy = "schema", targetEntity = HibSchemaVersionImpl.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<SchemaVersion> versions = new HashSet<>();

	@Override
	public SchemaVersion getLatestVersion() {
		return latestVersion;
	}

	@Override
	public void setLatestVersion(SchemaVersion nextVersion) {
		this.latestVersion = nextVersion;
	}

	public Iterable<Project> getLinkedProjects() {
		return Collections.unmodifiableSet(projects);
	}

	public Set<Project> getProjects() {
		return projects;
	}

	public Set<SchemaVersion> getVersions() {
		return versions;
	}
}
