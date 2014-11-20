package com.gentics.handler;

import io.vertx.core.Vertx;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public abstract class DeploymentManager {
	protected static Map<String, Set<String>> moduleDeployments = Collections.synchronizedMap(new HashMap<String, Set<String>>());

	protected Vertx vertx;

	public DeploymentManager(Vertx vertx) {
		this.vertx = vertx;
	}

	protected void registerModule(String moduleName, String deploymentId) {
		getDeployedModules(moduleName, true).add(deploymentId);
	}

	protected void unregisterModule(String moduleName, String deploymentId) {
		Set<String> deployedModules = getDeployedModules(moduleName, false);
		if (deployedModules != null) {
			deployedModules.remove(deploymentId);
			if (deployedModules.isEmpty()) {
				moduleDeployments.remove(moduleName);
			}
		}
	}

	protected Set<String> getDeployedModules(String moduleName, boolean create) {
		Set<String> moduleSet = moduleDeployments.get(moduleName);
		if (moduleSet == null && create) {
			moduleSet = Collections.synchronizedSet(new HashSet<String>());
			moduleDeployments.put(moduleName, moduleSet);
		}

		return moduleSet;
	}

	protected Map<String, Integer> getDeployedModules() {
		SortedMap<String, Integer> modules = new TreeMap<String, Integer>();
		for (Map.Entry<String, Set<String>> entry : moduleDeployments.entrySet()) {
			String moduleName = entry.getKey();
			Set<String> deployments = entry.getValue();

			modules.put(moduleName, deployments.size());
		}

		return modules;
	}
}
