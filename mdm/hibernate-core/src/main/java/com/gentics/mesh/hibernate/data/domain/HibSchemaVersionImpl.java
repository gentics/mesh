package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Container schema version entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = HibSchemaVersionImpl.TABLE_NAME)
@ElementTypeKey(ElementType.SCHEMAVERSION)
public class HibSchemaVersionImpl 
			extends AbstractHibFieldSchemaVersion<SchemaResponse, SchemaVersionModel, SchemaReference, Schema, SchemaVersion> 
			implements SchemaVersion, Serializable {

	private static final long serialVersionUID = -8916169864431516391L;

	public static final String TABLE_NAME = "schemaversion";

	@ManyToOne(targetEntity = HibSchemaImpl.class)
	private Schema schema;
	
	@OneToOne(targetEntity = HibSchemaVersionImpl.class, fetch = FetchType.LAZY)
	private SchemaVersion previousVersion;
	
	@OneToOne(targetEntity = HibSchemaVersionImpl.class, fetch = FetchType.LAZY)
	private SchemaVersion nextVersion;

	@OneToOne(fetch = FetchType.LAZY)
	private HibSchemaChangeImpl nextChange;

	@OneToOne(fetch = FetchType.LAZY)
	private HibSchemaChangeImpl previousChange;

	@OneToMany(mappedBy = "toSchemaVersion", targetEntity = HibJobImpl.class, fetch = FetchType.LAZY)
	private Set<HibJobImpl> toJobs = new HashSet<>();

	@OneToMany(mappedBy = "fromSchemaVersion", targetEntity = HibJobImpl.class, fetch = FetchType.LAZY)
	private Set<HibJobImpl> fromJobs = new HashSet<>();

	@Override
	public void setPreviousVersion(SchemaVersion version) {
		this.previousVersion = version;
	}

	@Override
	public void setNextVersion(SchemaVersion version) {
		this.nextVersion = version;
	}

	@Override
	public SchemaVersion getPreviousVersion() {
		return previousVersion;
	}

	@Override
	public SchemaVersion getNextVersion() {
		return nextVersion;
	}

	@Override
	public SchemaChange<?> getNextChange() {
		return nextChange == null ? null : nextChange.intoSchemaChange();
	}

	@Override
	public SchemaChange<?> getPreviousChange() {
		return previousChange == null ? null : previousChange.intoSchemaChange();
	}

	@Override
	public void setPreviousChange(SchemaChange<?> change) {
		this.previousChange = HibSchemaChangeImpl.intoEntity(change);
		if (previousChange != null) {
			previousChange.setNextSchemaContainerVersionInner(this);
		}
	}

	@Override
	public void setNextChange(SchemaChange<?> change) {
		this.nextChange = HibSchemaChangeImpl.intoEntity(change);
		if (nextChange != null) {
			nextChange.setPreviousContainerVersionInner(this);
		}
	}

	@Override
	public Iterable<? extends Job> referencedJobsViaFrom() {
		return new TraversalResult<>(fromJobs.iterator());
	}

	@Override
	public Schema getSchemaContainer() {
		return schema;
	}

	@Override
	public void setSchemaContainer(Schema container) {
		this.schema = container;
	}

	@Override
	public Iterable<? extends Job> referencedJobsViaTo() {
		return new TraversalResult<>(toJobs.iterator());
	}

	@Override
	public Class<? extends SchemaVersion> getContainerVersionClass() {
		return getClass();
	}

	@Override
	public Class<? extends Schema> getContainerClass() {
		return HibSchemaImpl.class;
	}
}
