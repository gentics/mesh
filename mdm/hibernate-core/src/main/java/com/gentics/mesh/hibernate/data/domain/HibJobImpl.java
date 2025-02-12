package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.job.JobResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.job.JobType;
import com.gentics.mesh.core.rest.job.JobWarningList;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.json.JsonUtil;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Job entity implementation for Gentics Mesh.
 * 
 * @author plyhun
 *
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Entity(name = "job")
@ElementTypeKey(ElementType.JOB)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class HibJobImpl extends AbstractHibUserTrackedElement<JobResponse> implements HibJob, Serializable {

	private static final long serialVersionUID = -1745332735121737689L;

	@ManyToOne(targetEntity = HibSchemaVersionImpl.class)
	private HibSchemaVersion toSchemaVersion;

	@ManyToOne(targetEntity = HibSchemaVersionImpl.class)
	private HibSchemaVersion fromSchemaVersion;

	@ManyToOne(targetEntity = HibMicroschemaVersionImpl.class)
	private HibMicroschemaVersion toMicroschemaVersion;

	@ManyToOne(targetEntity = HibMicroschemaVersionImpl.class)
	private HibMicroschemaVersion fromMicroschemaVersion;

	@OneToOne(targetEntity = HibBranchImpl.class)
	private HibBranch branch;

	@Enumerated(EnumType.STRING)
	private JobType jobType;

	@Enumerated(EnumType.STRING)
	private JobStatus jobStatus;

	private String errorMessage;

	private Long startTimestamp;

	private Long stopTimestamp;

	private long completionCount;

	private String nodeName;

	@Column(length = ERROR_DETAIL_MAX_LENGTH)
	private String warnings;

	@Column(length = ERROR_DETAIL_MAX_LENGTH)
	private String errorDetail;

	@Override
	public HibBranch getBranch() {
		return branch;
	}

	@Override
	public void setBranch(HibBranch branch) {
		this.branch = branch;
	}

	@Override
	public JobType getType() {
		return jobType;
	}

	@Override
	public void setType(JobType type) {
		this.jobType = type;
	}

	@Override
	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public void setErrorMessage(String message) {
		errorMessage = message;
	}

	@Override
	public String getErrorDetail() {
		return errorDetail;
	}

	@Override
	public void setErrorDetail(String info) {
		errorDetail = info;
	}

	@Override
	public JobStatus getStatus() {
		return jobStatus;
	}

	@Override
	public void setStatus(JobStatus status) {
		this.jobStatus = status;
	}

	@Override
	public HibSchemaVersion getFromSchemaVersion() {
		return fromSchemaVersion;
	}

	@Override
	public void setFromSchemaVersion(HibSchemaVersion version) {
		fromSchemaVersion = version;
	}

	@Override
	public HibSchemaVersion getToSchemaVersion() {
		return toSchemaVersion;
	}

	@Override
	public void setToSchemaVersion(HibSchemaVersion version) {
		toSchemaVersion = version;
	}

	@Override
	public HibMicroschemaVersion getFromMicroschemaVersion() {
		return fromMicroschemaVersion;
	}

	@Override
	public void setFromMicroschemaVersion(HibMicroschemaVersion fromVersion) {
		fromMicroschemaVersion = fromVersion;
	}

	@Override
	public HibMicroschemaVersion getToMicroschemaVersion() {
		return toMicroschemaVersion;
	}

	@Override
	public void setToMicroschemaVersion(HibMicroschemaVersion toVersion) {
		toMicroschemaVersion = toVersion;
	}

	@Override
	public Long getStartTimestamp() {
		return startTimestamp;
	}

	@Override
	public void setStartTimestamp(Long date) {
		startTimestamp = date;
	}

	@Override
	public Long getStopTimestamp() {
		return stopTimestamp;
	}

	@Override
	public void setStopTimestamp(Long date) {
		stopTimestamp = date;
	}

	@Override
	public long getCompletionCount() {
		return completionCount;
	}

	@Override
	public void setCompletionCount(long count) {
		completionCount = count;
	}

	@Override
	public String getNodeName() {
		return nodeName;
	}

	@Override
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	@Override
	public JobWarningList getWarnings() {
		if (warnings == null) {
			return new JobWarningList();
		} else {
			return JsonUtil.readValue(warnings, JobWarningList.class);
		}
	}

	@Override
	public void setWarnings(JobWarningList warnings) {
		this.warnings = warnings.toJson();
	}
}
