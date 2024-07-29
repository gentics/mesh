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
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
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
		extends AbstractHibFieldSchemaVersion<MicroschemaResponse, MicroschemaVersionModel, MicroschemaReference, HibMicroschema, HibMicroschemaVersion> 
		implements HibMicroschemaVersion, Serializable {

	private static final long serialVersionUID = -3648238507867008944L;

	public static final String TABLE_NAME = "microschemaversion";

	@ManyToOne(targetEntity = HibMicroschemaImpl.class)
	private HibMicroschema microschema;

	@OneToOne(targetEntity = HibMicroschemaVersionImpl.class, fetch = FetchType.LAZY)
	private HibMicroschemaVersion previousVersion;
	
	@OneToOne(targetEntity = HibMicroschemaVersionImpl.class, fetch = FetchType.LAZY)
	private HibMicroschemaVersion nextVersion;

	@OneToOne(fetch = FetchType.LAZY)
	private HibSchemaChangeImpl previousChange;

	@OneToOne(fetch = FetchType.LAZY)
	private HibSchemaChangeImpl nextChange;

	@OneToMany(mappedBy = "toMicroschemaVersion", targetEntity = HibJobImpl.class, fetch = FetchType.LAZY)
	private Set<HibJobImpl> toJobs = new HashSet<>();

	@OneToMany(mappedBy = "fromMicroschemaVersion", targetEntity = HibJobImpl.class, fetch = FetchType.LAZY)
	private Set<HibJobImpl> fromJobs = new HashSet<>();

	@Override
	public void setPreviousVersion(HibMicroschemaVersion version) {
		this.previousVersion = version;
	}

	@Override
	public void setNextVersion(HibMicroschemaVersion version) {
		this.nextVersion = version;
	}

	@Override
	public HibMicroschemaVersion getPreviousVersion() {
		return previousVersion;
	}

	@Override
	public HibMicroschemaVersion getNextVersion() {
		return nextVersion;
	}

	@Override
	public Class<? extends HibMicroschemaVersion> getContainerVersionClass() {
		return getClass();
	}

	@Override
	public Class<? extends HibMicroschema> getContainerClass() {
		return HibMicroschemaImpl.class;
	}

	@Override
	public HibSchemaChange<?> getNextChange() {
		return nextChange == null ? null : nextChange.intoSchemaChange();
	}

	@Override
	public HibSchemaChange<?> getPreviousChange() {
		return previousChange == null ? null : previousChange.intoSchemaChange();
	}

	@Override
	public void setPreviousChange(HibSchemaChange<?> change) {
		this.previousChange = HibSchemaChangeImpl.intoEntity(change);
		if (previousChange != null) {
			previousChange.setNextSchemaContainerVersionInner(this);
		}
	}

	@Override
	public void setNextChange(HibSchemaChange<?> change) {
		this.nextChange = HibSchemaChangeImpl.intoEntity(change);
		if (nextChange != null) {
			nextChange.setPreviousContainerVersionInner(this);
		}
	}

	@Override
	public Iterable<? extends HibJob> referencedJobsViaTo() {
		return new TraversalResult<>(toJobs.iterator());
	}

	@Override
	public Iterable<? extends HibJob> referencedJobsViaFrom() {
		return new TraversalResult<>(fromJobs.iterator());
	}

	@Override
	public HibMicroschema getSchemaContainer() {
		return microschema;
	}

	@Override
	public void setSchemaContainer(HibMicroschema container) {
		this.microschema = container;
	}
}
