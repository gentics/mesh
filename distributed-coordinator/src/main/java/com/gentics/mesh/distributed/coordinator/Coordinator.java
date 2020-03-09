package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

/**
 * The coordinator manages the elector and keeps track of the currently configured coordination mode.
 */
@Singleton
public class Coordinator {

	private final MasterElector elector;
	private CoordinatorMode mode = CoordinatorMode.DISABLED;

	@Inject
	public Coordinator(MasterElector elector, MeshOptions options) {
		this.elector = elector;
		this.mode = options.getClusterOptions().getCoordinatorMode();
	}

	public MasterServer getMasterMember() {
		return elector.getMasterMember();
	}

	public CoordinatorMode getCoordinatorMode() {
		return mode;
	}

	public Coordinator setCoordinatorMode(CoordinatorMode mode) {
		this.mode = mode;
		return this;
	}

	public void setMaster() {
		elector.setMaster();
	}

	public CoordinatorConfig loadConfig() {
		return new CoordinatorConfig().setMode(mode);
	}

	/**
	 * Update the coordinator configuration.
	 * 
	 * @param config
	 */
	public void updateConfig(CoordinatorConfig config) {
		CoordinatorMode newMode = config.getMode();
		if (newMode != null) {
			this.mode = newMode;
		}
	}

	/**
	 * Check whether the node that is managed by this coordinator is eligible for master election.
	 * 
	 * @return
	 */
	public boolean isElectable() {
		return elector.isElectable();
	}

}
