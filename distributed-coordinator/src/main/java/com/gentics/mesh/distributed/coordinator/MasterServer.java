package com.gentics.mesh.distributed.coordinator;

/**
 * Master server POJO which holds information we know about the elected master.
 */
public class MasterServer {

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
	public MasterServer(String name, String host, int port, boolean self) {
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

	@Override
	public String toString() {
		return "Node: " + host + ":" + port;
	}

}
