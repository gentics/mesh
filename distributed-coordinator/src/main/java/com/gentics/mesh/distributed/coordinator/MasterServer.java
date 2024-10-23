package com.gentics.mesh.distributed.coordinator;

import java.util.UUID;

import com.gentics.mesh.util.UUIDUtil;

/**
 * Master server POJO which holds information we know about the elected master.
 */
public class MasterServer {

	private UUID uuid;
	private String name;
	private String host;
	private int port;
	private boolean self;

	/**
	 * Create a master server object.
	 *
	 * @param name
	 *            Name of the node
	 * @param host
	 *            Hostname of the server which can be used to reach the REST API
	 * @param port
	 *            HTTP port
	 * @param self
	 *            Flag which indicates whether we are the master
	 */
	public MasterServer(UUID uuid, String name, String host, int port, boolean self) {
		this.setUuid(uuid);
		this.name = name;
		this.host = host;
		this.port = port;
		this.self = self;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isSelf() {
		return self;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	@Override
	public String toString() {
		return "Node: " + host + ":" + port + ":" + UUIDUtil.toShortUuid(uuid);
	}
}
