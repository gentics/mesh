package com.gentics.mesh.hibernate.data.domain;

import static com.gentics.mesh.hibernate.data.domain.HibMicroschemaVersionImpl.TABLE_NAME;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;

/**
 * Microschema version entity implementation for Enterprise Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = TABLE_NAME)
@ElementTypeKey(ElementType.MICROSCHEMAVERSION)
public class HibMicroschemaVersionImpl 
		extends AbstractHibFieldSchemaVersion<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, Microschema, MicroschemaVersion> 
		implements MicroschemaVersion, Serializable {

	private static final long serialVersionUID = -3648238507867008944L;

	public static final String TABLE_NAME = "microschemaversion";

	@ManyToOne(targetEntity = HibMicroschemaImpl.class)
	private Microschema microschema;

	@OneToOne(targetEntity = HibMicroschemaVersionImpl.class, fetch = FetchType.LAZY)
	private MicroschemaVersion previousVersion;
	
	@OneToOne(targetEntity = HibMicroschemaVersionImpl.class, fetch = FetchType.LAZY)
	private MicroschemaVersion nextVersion;

	@OneToOne(fetch = FetchType.LAZY)
	private HibSchemaChangeImpl previousChange;

	@OneToOne(fetch = FetchType.LAZY)
	private HibSchemaChangeImpl nextChange;

	@OneToMany(mappedBy = "toMicroschemaVersion", targetEntity = HibJobImpl.class, fetch = FetchType.LAZY)
	private Set<HibJobImpl> toJobs = new HashSet<>();

	@OneToMany(mappedBy = "fromMicroschemaVersion", targetEntity = HibJobImpl.class, fetch = FetchType.LAZY)
	private Set<HibJobImpl> fromJobs = new HashSet<>();

	@Override
	public void setPreviousVersion(MicroschemaVersion version) {
		this.previousVersion = version;
	}

	@Override
	public void setNextVersion(MicroschemaVersion version) {
		this.nextVersion = version;
	}

	@Override
	public MicroschemaVersion getPreviousVersion() {
		return previousVersion;
	}

	@Override
	public MicroschemaVersion getNextVersion() {
		return nextVersion;
	}

	@Override
	public Class<? extends MicroschemaVersion> getContainerVersionClass() {
		return getClass();
	}

	@Override
	public Class<? extends Microschema> getContainerClass() {
		return HibMicroschemaImpl.class;
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
	public Iterable<? extends Job> referencedJobsViaTo() {
		return new TraversalResult<>(toJobs.iterator());
	}

	@Override
	public Iterable<? extends Job> referencedJobsViaFrom() {
		return new TraversalResult<>(fromJobs.iterator());
	}

	@Override
	public Microschema getSchemaContainer() {
		return microschema;
	}

	@Override
	public void setSchemaContainer(Microschema container) {
		this.microschema = container;
	}
}
