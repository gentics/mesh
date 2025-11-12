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
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Container schema entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(
		name = "schema.rest",
		attributeNodes = {
			@NamedAttributeNode("latestVersion")
		}
	),
	@NamedEntityGraph(
		name = "schema.load",
		attributeNodes = {
			@NamedAttributeNode("latestVersion")
		}
	)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "schema")
@ElementTypeKey(ElementType.SCHEMA)
public class HibSchemaImpl extends AbstractHibUserTrackedElement<SchemaResponse> implements HibSchema, Serializable {

	private static final long serialVersionUID = 8334546969092825031L;

	@ManyToMany(mappedBy = "schemas", targetEntity = HibProjectImpl.class, fetch = FetchType.LAZY)
	private Set<HibProject> projects = new HashSet<>();

	@OneToOne(targetEntity = HibSchemaVersionImpl.class)
	private HibSchemaVersion latestVersion;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@OneToMany(mappedBy = "schema", targetEntity = HibSchemaVersionImpl.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<HibSchemaVersion> versions = new HashSet<>();

	@Override
	public HibSchemaVersion getLatestVersion() {
		return latestVersion;
	}

	@Override
	public void setLatestVersion(HibSchemaVersion nextVersion) {
		this.latestVersion = nextVersion;
	}

	public Iterable<HibProject> getLinkedProjects() {
		return Collections.unmodifiableSet(projects);
	}

	public Set<HibProject> getProjects() {
		return projects;
	}

	public Set<HibSchemaVersion> getVersions() {
		return versions;
	}
}
