package com.gentics.mesh.distributed.coordinator;

import java.io.Serializable;
import java.time.Instant;

/**
 * Info of the Mesh cluster instance.
 */
public final class MeshMemberInfo implements Serializable {
	private static final long serialVersionUID = 6977949110409898772L;

	private final String name;
	private final int port;
	private final Instant startedAt;
	private boolean isMaster;

	public MeshMemberInfo(String name, int port, Instant startedAt) {
		super();
		this.name = name;
		this.port = port;
		this.startedAt = startedAt;
	}

	public boolean isMaster() {
		return isMaster;
	}

	void setMaster(boolean isMaster) {
		this.isMaster = isMaster;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}

	public Instant getStartedAt() {
		return startedAt;
	}
}