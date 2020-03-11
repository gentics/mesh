package com.gentics.mesh.etc.config;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.cluster.CoordinationTopology;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * POJO for the mesh cluster options
 */
@GenerateDocumentation
public class ClusterOptions implements Option {

	public static final boolean ENABLED = true;
	public static final boolean DISABLED = false;

	public static final boolean DEFAULT_CLUSTER_MODE = DISABLED;
	public static final int DEFAULT_VERTX_PORT = 4848;
	public static final long DEFAULT_TOPOLOGY_LOCK_TIMEOUT = 0;

	public static final String MESH_CLUSTER_NETWORK_HOST_ENV = "MESH_CLUSTER_NETWORK_HOST";
	public static final String MESH_CLUSTER_ENABLED_ENV = "MESH_CLUSTER_ENABLED";
	public static final String MESH_CLUSTER_NAME_ENV = "MESH_CLUSTER_NAME";
	public static final String MESH_CLUSTER_VERTX_PORT_ENV = "MESH_CLUSTER_VERTX_PORT";
	public static final String MESH_CLUSTER_COORDINATOR_MODE_ENV = "MESH_CLUSTER_COORDINATOR_MODE";
	public static final String MESH_CLUSTER_COORDINATOR_REGEX_ENV = "MESH_CLUSTER_COORDINATOR_REGEX";
	public static final String MESH_CLUSTER_TOPOLOGY_LOCK_TIMEOUT_ENV = "MESH_CLUSTER_TOPOLOGY_LOCK_TIMEOUT";
	public static final String MESH_CLUSTER_COORDINATOR_TOPOLOGY_ENV = "MESH_CLUSTER_COORDINATOR_TOPOLOGY";

	@JsonProperty(required = false)
	@JsonPropertyDescription("IP or host which is used to announce and reach the instance in the cluster. Gentics Mesh will try to determine the IP automatically but you may use this setting to override this automatic IP handling.")
	@EnvironmentVariable(name = MESH_CLUSTER_NETWORK_HOST_ENV, description = "Override the cluster network host.")
	private String networkHost;

	// TODO use public and bind host - https://www.prjhub.com/#/issues/9058, https://www.elastic.co/guide/en/elasticsearch/reference/2.0/modules-network.html
	// private String publicHost;
	// private String bindHost;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag to enable or disable the cluster mode.")
	@EnvironmentVariable(name = MESH_CLUSTER_ENABLED_ENV, description = "Override cluster enabled flag.")
	private boolean enabled = DEFAULT_CLUSTER_MODE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the cluster. Only instances with a common cluster name will form a cluster.")
	@EnvironmentVariable(name = MESH_CLUSTER_NAME_ENV, description = "Override the cluster name.")
	private String clusterName;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Port used by Vert.x for the eventbus server. Default: " + DEFAULT_VERTX_PORT)
	@EnvironmentVariable(name = MESH_CLUSTER_VERTX_PORT_ENV, description = "Override the vert.x eventbus server port.")
	private Integer vertxPort = DEFAULT_VERTX_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The coordinator mode will add an additional request controller plane which will internally process requests in-between cluster nodes. Default: DISABLED")
	@EnvironmentVariable(name = MESH_CLUSTER_COORDINATOR_MODE_ENV, description = "Override the cluster coordinator mode.")
	private CoordinatorMode coordinatorMode = CoordinatorMode.DISABLED;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The coordinator regex can be used to control which nodes in the cluster are eligible to be elected in a coordinator master election. When left empty all database master nodes are eligible.")
	@EnvironmentVariable(name = MESH_CLUSTER_COORDINATOR_REGEX_ENV, description = "Override the cluster coordinator regex.")
	private String coordinatorRegex;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Define the timeout in ms for the topology lock. The topology lock will lock all transactions whenever the cluster topology changes. Default: "
		+ DEFAULT_TOPOLOGY_LOCK_TIMEOUT + ". A value of 0 will disable the locking mechanism.")
	@EnvironmentVariable(name = MESH_CLUSTER_TOPOLOGY_LOCK_TIMEOUT_ENV, description = "Override the cluster topology lock timeout in ms.")
	private long topologyLockTimeout = DEFAULT_TOPOLOGY_LOCK_TIMEOUT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The coordinator topology setting controls whether the coordinator should manage the cluster topology. By default no cluster topology management will be done.")
	@EnvironmentVariable(name = MESH_CLUSTER_COORDINATOR_TOPOLOGY_ENV, description = "Override the cluster coordinator topology management mode.")
	private CoordinationTopology coordinatorTopology = CoordinationTopology.UNMANAGED;

	public boolean isEnabled() {
		return enabled;
	}

	public ClusterOptions setEnabled(boolean flag) {
		this.enabled = flag;
		return this;
	}

	public String getNetworkHost() {
		return networkHost;
	}

	public ClusterOptions setNetworkHost(String networkHost) {
		this.networkHost = networkHost;
		return this;
	}

	public String getClusterName() {
		return clusterName;
	}

	public ClusterOptions setClusterName(String clusterName) {
		this.clusterName = clusterName;
		return this;
	}

	public Integer getVertxPort() {
		return vertxPort;
	}

	public ClusterOptions setVertxPort(Integer vertxPort) {
		this.vertxPort = vertxPort;
		return this;
	}

	public CoordinatorMode getCoordinatorMode() {
		return coordinatorMode;
	}

	public ClusterOptions setCoordinatorMode(CoordinatorMode coordinatorMode) {
		this.coordinatorMode = coordinatorMode;
		return this;
	}

	public String getCoordinatorRegex() {
		return coordinatorRegex;
	}

	public ClusterOptions setCoordinatorRegex(String coordinatorRegex) {
		this.coordinatorRegex = coordinatorRegex;
		return this;
	}

	public long getTopologyLockTimeout() {
		return topologyLockTimeout;
	}

	public ClusterOptions setTopologyLockTimeout(long topologyLockTimeout) {
		this.topologyLockTimeout = topologyLockTimeout;
		return this;
	}

	public CoordinationTopology getCoordinatorTopology() {
		return coordinatorTopology;
	}

	public ClusterOptions setCoordinatorTopology(CoordinationTopology coordinatorTopology) {
		this.coordinatorTopology = coordinatorTopology;
		return this;
	}

	/**
	 * Validate the options.
	 * 
	 * @param meshOptions
	 */
	public void validate(MeshOptions meshOptions) {
		if (isEnabled()) {
			Objects.requireNonNull(getClusterName(), "No cluster.clusterName was specified within mesh options.");
			Objects.requireNonNull(meshOptions.getNodeName(), "No nodeName was specified within mesh options.");
		}
	}

}
