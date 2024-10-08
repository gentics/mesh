package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.annotation.Setter;
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

	/**
	 * Get the current master server, may be null
	 * @return current master, may be null
	 */
	public MasterServer getMasterMember() {
		return elector.getMasterMember();
	}

	public CoordinatorMode getCoordinatorMode() {
		return mode;
	}

	@Setter
	public Coordinator setCoordinatorMode(CoordinatorMode mode) {
		this.mode = mode;
		return this;
	}

	@Setter
	public void setMaster() {
		elector.setMaster();
	}

	@Getter
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

	/**
	 * Check if the current node is the master.
	 * 
	 * @return
	 */
	public boolean isMaster() {
		MasterServer masterMember = getMasterMember();
		if (masterMember != null) {
			return masterMember.isSelf();
		} else {
			return false;
		}
	}
}
