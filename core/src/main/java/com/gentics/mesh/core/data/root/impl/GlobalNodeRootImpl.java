//package com.gentics.mesh.core.data.root.impl;
//
//import java.util.List;
//import java.util.stream.Stream;
//
//import javax.inject.Inject;
//import javax.inject.Singleton;
//
//import org.apache.commons.lang.NotImplementedException;
//
//import com.gentics.madl.tx.Tx;
//import com.gentics.mesh.context.InternalActionContext;
//import com.gentics.mesh.core.data.MeshAuthUser;
//import com.gentics.mesh.core.data.Project;
//import com.gentics.mesh.core.data.User;
//import com.gentics.mesh.core.data.node.Node;
//import com.gentics.mesh.core.data.node.impl.NodeImpl;
//import com.gentics.mesh.core.data.page.Page;
//import com.gentics.mesh.core.data.page.impl.DynamicStreamPageImpl;
//import com.gentics.mesh.core.data.root.GlobalNodeRoot;
//import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
//import com.gentics.mesh.error.InvalidArgumentException;
//import com.gentics.mesh.event.EventQueueBatch;
//import com.gentics.mesh.graphdb.spi.Database;
//import com.gentics.mesh.madl.traversal.TraversalResult;
//import com.gentics.mesh.parameter.PagingParameters;
//import com.syncleus.ferma.FramedGraph;
//
//@Singleton
//public class GlobalNodeRootImpl extends AbstractRootVertex<Node> implements GlobalNodeRoot {
//
//	private final Database db;
//
//	@Inject
//	public GlobalNodeRootImpl(Database db) {
//		this.db = db;
//	}
//
//	@Override
//	public Page<? extends Node> findAll(MeshAuthUser requestUser, List<String> languageTags, PagingParameters pagingInfo)
//		throws InvalidArgumentException {
//		return new DynamicStreamPageImpl<Node>(findAll().stream(), pagingInfo);
//	}
//
//	@Override
//	public TraversalResult<? extends Node> findAll() {
//		Stream<? extends Node> stream = db().type().findAll(getPersistanceClass());
//		return new TraversalResult<>(stream);
//	}
//	
//	@Override
//	public Node findByUuid(String uuid) {
//		return db().index().findByUuid(NodeImpl.class, uuid);
//	}
//
//	@Override
//	public FramedGraph getGraph() {
//		return Tx.get().getGraph();
//	}
//
//	@Override
//	public Node create(User user, SchemaContainerVersion container, Project project, String uuid) {
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public void addNode(Node node) {
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public void removeNode(Node node) {
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Node create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
//		throw new NotImplementedException();
//	}
//
//	@Override
//	public Class<? extends Node> getPersistanceClass() {
//		return NodeImpl.class;
//	}
//
//	@Override
//	public String getRootLabel() {
//		return null;
//	}
//
//	@Override
//	public long computeCount() {
//		return db().type().count(getPersistanceClass());
//	}
//
//	@Override
//	public Database db() {
//		return db;
//	}
//
//}
