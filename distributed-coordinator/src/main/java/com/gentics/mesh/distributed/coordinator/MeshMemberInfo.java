package com.gentics.mesh.distributed.coordinator;

import java.io.Serializable;

final class MeshMemberInfo implements Serializable {
	private static final long serialVersionUID = 6977949110409898772L;

	public String name;
	public int port;
	public boolean isMaster;
}