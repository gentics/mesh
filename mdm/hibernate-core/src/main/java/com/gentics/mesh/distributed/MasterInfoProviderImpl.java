package com.gentics.mesh.distributed;

import javax.inject.Inject;

import com.gentics.mesh.distributed.coordinator.MasterElector;

/**
 * Implementation of {@link MasterInfoProvider}
 */
public class MasterInfoProviderImpl implements MasterInfoProvider {
	private final MasterElector masterElector;

	@Inject
	public MasterInfoProviderImpl(MasterElector masterElector) {
		this.masterElector = masterElector;
	}

	@Override
	public boolean isMaster() {
		return masterElector.isMaster();
	}
}
