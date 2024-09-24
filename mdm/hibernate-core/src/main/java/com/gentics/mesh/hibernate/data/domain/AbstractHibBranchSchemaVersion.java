package com.gentics.mesh.hibernate.data.domain;

import javax.annotation.Nonnull;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.database.HibernateTx;

/**
 * Common part for branch (micro)schema version edge.
 * 
 * @author plyhun
 *
 * @param <SCV>
 */
@MappedSuperclass
public abstract class AbstractHibBranchSchemaVersion<SCV extends AbstractHibFieldSchemaVersion<?, ?, ?, ?, ?>> extends AbstractHibBaseElement {

	@ManyToOne(optional = false)
	private HibBranchImpl branch;

	@ManyToOne(optional = false)
	private SCV version;

	private boolean active;
	private JobStatus migrationStatus;
	private String jobUuid;

	public AbstractHibBranchSchemaVersion() {}

	public AbstractHibBranchSchemaVersion(HibernateTx tx, HibBranchImpl branch, SCV version) {
		setDbUuid(tx.uuidGenerator().generateType1UUID());
		this.branch = branch;
		this.version = version;
	}

	public void setBranch(@Nonnull HibBranchImpl branch) {
		this.branch = branch;
	}

	public void setVersion(@Nonnull SCV version) {
		this.version = version;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public JobStatus getMigrationStatus() {
		return migrationStatus;
	}

	public void setMigrationStatus(JobStatus migrationStatus) {
		this.migrationStatus = migrationStatus;
	}

	public String getJobUuid() {
		return jobUuid;
	}

	public void setJobUuid(String jobUuid) {
		this.jobUuid = jobUuid;
	}

	public HibBranchImpl getBranch() {
		return branch;
	}

	public SCV getVersion() {
		return version;
	}
}
