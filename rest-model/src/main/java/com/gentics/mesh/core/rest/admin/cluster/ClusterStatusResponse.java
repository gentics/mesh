package com.gentics.mesh.core.rest.admin.cluster;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for cluster status.
 */
public class ClusterStatusResponse implements RestModel {

	private List<ClusterInstanceInfoModel> instances = new ArrayList<>();

	public List<ClusterInstanceInfoModel> getInstances() {
		return instances;
	}

}
