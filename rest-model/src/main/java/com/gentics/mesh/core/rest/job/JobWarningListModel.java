package com.gentics.mesh.core.rest.job;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.job.warning.JobWarningModel;

/**
 * POJO for warnings within a {@link JobResponse}
 */
public class JobWarningListModel implements RestModel {

	private List<JobWarningModel> data = new ArrayList<>();

	public JobWarningListModel() {
	}

	public List<JobWarningModel> getData() {
		return data;
	}

	public void setWarnings(List<JobWarningModel> data) {
		this.data = data;
	}

	@Setter
	public void add(JobWarningModel warning) {
		this.data.add(warning);
	}

}
