package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedGraph;

public class AbstractTrx {

	protected static ThreadLocal<FramedGraph> localGraph = new ThreadLocal<>();

	protected FramedGraph oldLocalGraph;
	protected FramedGraph currentGraph;
	
	public static void setLocalGraph(FramedGraph graph) {
		AbstractTrx.localGraph.set(graph);
	}

	public static FramedGraph getFramedLocalGraph() {
		return getLocalGraph();
	}

	public static FramedGraph getLocalGraph() {
		return AbstractTrx.localGraph.get();
	}

	public FramedGraph getGraph() {
		return currentGraph;
	}

}
