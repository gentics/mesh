package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.etc.config.MeshOptions;

/**
 * The coordinator manages the elector and keeps track of the currently configured coordination mode.
 */
@Singleton
public class Coordinator {

	private final MasterElector elector;

	@Inject
	public Coordinator(MasterElector elector, MeshOptions options) {
		this.elector = elector;
	}

	/**
	 * Get the current master server, may be null
	 * @return current master, may be null
	 */
	public MasterServer getMasterMember() {
		return elector.getMasterMember();
	}

	@Setter
	public void setMaster() {
		elector.setMaster();
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
