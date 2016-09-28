package com.gentics.mesh.graphdb;

import com.syncleus.ferma.FramedGraph;

public abstract class AbstractNoTx<T extends FramedGraph> extends AbstractTrxBase<T> implements NoTx {

}
