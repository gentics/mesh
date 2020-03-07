package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.admin.cluster.coordinator.CoordinatorConfig;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

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

	public void electMaster() {
		elector.electMaster();
	}

	public CoordinatorConfig loadConfig() {
		return new CoordinatorConfig().setMode(mode);
	}

	public void updateConfig(CoordinatorConfig config) {
		CoordinatorMode newMode = config.getMode();
		if (newMode != null) {
			this.mode = newMode;
		}
	}

}
