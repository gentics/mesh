package com.gentics.mesh.graphdb;

import static com.gentics.mesh.Events.EVENT_CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.Events.EVENT_CLUSTER_NODE_JOINED;
import static com.gentics.mesh.Events.EVENT_CLUSTER_NODE_JOINING;
import static com.gentics.mesh.Events.EVENT_CLUSTER_NODE_LEFT;

import com.gentics.mesh.Mesh;
import com.orientechnologies.orient.server.distributed.ODistributedLifecycleListener;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;

import io.vertx.core.eventbus.EventBus;

/**
 * Listener for OrientDB cluster specific events. The listener relays the events via messages to the eventbus.
 */
public class TopologyEventBridge implements ODistributedLifecycleListener {

	private EventBus eb;

	public TopologyEventBridge() {
		this.eb = Mesh.mesh().getVertx().eventBus();
	}

	@Override
	public boolean onNodeJoining(String nodeName) {
		eb.send(EVENT_CLUSTER_NODE_JOINING, nodeName);
		//TODO validate that the joining node uses the same mesh version as our node. Otherwise the join should be denied.
		return true;
	}

	@Override
	public void onNodeJoined(String iNode) {
		eb.send(EVENT_CLUSTER_NODE_JOINED, iNode);
	}

	@Override
	public void onNodeLeft(String iNode) {
		eb.send(EVENT_CLUSTER_NODE_LEFT, iNode);
	}

	@Override
	public void onDatabaseChangeStatus(String iNode, String iDatabaseName, DB_STATUS iNewStatus) {
		eb.send(EVENT_CLUSTER_DATABASE_CHANGE_STATUS, iNode + "." + iDatabaseName + ":" + iNewStatus.name());
	}

}
