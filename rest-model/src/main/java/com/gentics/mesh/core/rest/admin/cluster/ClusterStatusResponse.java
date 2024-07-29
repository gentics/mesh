package com.gentics.mesh.core.rest.admin.cluster;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for cluster status.
 */
public class ClusterStatusResponse implements RestModel {

	private List<ClusterInstanceInfo> instances = new ArrayList<>();

	public List<ClusterInstanceInfo> getInstances() {
		return instances;
	}

}
