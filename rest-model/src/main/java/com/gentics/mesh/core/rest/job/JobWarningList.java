package com.gentics.mesh.core.rest.job;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.job.warning.JobWarning;

/**
 * POJO for warnings within a {@link JobResponse}
 */
public class JobWarningList implements RestModel {

	private List<JobWarning> data = new ArrayList<>();

	public JobWarningList() {
	}

	public List<JobWarning> getData() {
		return data;
	}

	public void setWarnings(List<JobWarning> data) {
		this.data = data;
	}

	@Setter
	public void add(JobWarning warning) {
		this.data.add(warning);
	}

}
