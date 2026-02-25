package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

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
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Microschema entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@NamedEntityGraphs({
	@NamedEntityGraph(
			name = "microschema.rest",
			attributeNodes = {
					@NamedAttributeNode("latestVersion")
			}
	),
	@NamedEntityGraph(
			name = "microschema.load",
			attributeNodes = {
					@NamedAttributeNode("latestVersion")
			}
	)
})
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = HibMicroschemaImpl.TABLE_NAME)
@ElementTypeKey(ElementType.MICROSCHEMA)
public class HibMicroschemaImpl extends AbstractHibUserTrackedElement<MicroschemaResponse> implements HibMicroschema, Serializable {

	private static final long serialVersionUID = 6185066222311338821L;

	public static final String TABLE_NAME = "microschema";

	@ManyToMany(mappedBy = "microschemas", targetEntity = HibProjectImpl.class, fetch = FetchType.LAZY)
	private Set<HibProject> projects = new HashSet<>();

	@OneToOne(targetEntity = HibMicroschemaVersionImpl.class)
	private HibMicroschemaVersion latestVersion;

	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	@OneToMany(mappedBy = "microschema", targetEntity = HibMicroschemaVersionImpl.class, cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
	private Set<HibMicroschemaVersion> versions = new HashSet<>();

	@Override
	public HibMicroschemaVersion getLatestVersion() {
		return latestVersion;
	}

	@Override
	public void setLatestVersion(HibMicroschemaVersion version) {
		if (version != null) {
			versions.add(version);
		}
		this.latestVersion = version;
	}

	public Iterable<HibProject> getLinkedProjects() {
		return Collections.unmodifiableSet(projects);
	}

	@Override
	public MicroschemaResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		String version = ac.getVersioningParameters().getVersion();
		// Delegate transform call to latest version
		if (version == null || version.equals("draft")) {
			return getLatestVersion().transformToRestSync(ac, level, languageTags);
		} else {
			HibMicroschemaVersion foundVersion = Tx.get().microschemaDao().findVersionByRev(this, version);
			if (foundVersion == null) {
				throw error(NOT_FOUND, "object_not_found_for_uuid_version", getUuid(), version);
			}
			return foundVersion.transformToRestSync(ac, level, languageTags);
		}
	}

	public Set<HibMicroschemaVersion> getVersions() {
		return versions;
	}

	public void setVersions(Set<HibMicroschemaVersion> versions) {
		this.versions = versions;
	}

	public Set<HibProject> getProjects() {
		return projects;
	}
}
