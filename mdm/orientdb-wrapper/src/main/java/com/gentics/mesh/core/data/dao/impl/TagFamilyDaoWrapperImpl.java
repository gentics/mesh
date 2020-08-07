package com.gentics.mesh.core.data.dao.impl;

import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.frame.EdgeFrame;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.madl.frame.VertexFrame;
import com.gentics.mesh.madl.tp3.mock.GraphTraversal;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.google.gson.JsonObject;
import com.syncleus.ferma.ClassInitializer;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.TEdge;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Vertex;

import dagger.Lazy;
import io.reactivex.Single;
import io.vertx.core.Vertx;

// TODO there is no tag family root since the tag itself is the root. 
public class TagFamilyDaoWrapperImpl implements TagFamilyDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public TagFamilyDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().tagFamilyRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().tagFamilyRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().tagFamilyRoot().setUuid(uuid);
	}

	public String getAPIPath(TagFamily element, InternalActionContext ac) {
		return boot.get().tagFamilyRoot().getAPIPath(element, ac);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public TagFamily create(String name, User user) {
		return boot.get().tagFamilyRoot().create(name, user);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().tagFamilyRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().tagFamilyRoot().getUuid();
	}

	public Single<TagFamilyResponse> transformToRest(TagFamily element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().tagFamilyRoot().transformToRest(element, ac, level, languageTags);
	}

	public Vertex getVertex() {
		return boot.get().tagFamilyRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().tagFamilyRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().tagFamilyRoot().property(name);
	}

	public TagFamily create(String name, User user, String uuid) {
		return boot.get().tagFamilyRoot().create(name, user, uuid);
	}

	public void delete(BulkActionContext bac) {
		boot.get().tagFamilyRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().tagFamilyRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().tagFamilyRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().tagFamilyRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().tagFamilyRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().tagFamilyRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().tagFamilyRoot().out(labels);
	}

	public void remove() {
		boot.get().tagFamilyRoot().remove();
	}

	public void delete() {
		boot.get().tagFamilyRoot().delete();
	}

	public TagFamilyResponse transformToRestSync(TagFamily element, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().tagFamilyRoot().transformToRestSync(element, ac, level, languageTags);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().tagFamilyRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().tagFamilyRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().tagFamilyRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().tagFamilyRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public void removeTagFamily(TagFamily tagFamily) {
		boot.get().tagFamilyRoot().removeTagFamily(tagFamily);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().tagFamilyRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().tagFamilyRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().tagFamilyRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().tagFamilyRoot().addFramedEdge(label, inVertex, kind);
	}

	public void addTagFamily(TagFamily tagFamily) {
		boot.get().tagFamilyRoot().addTagFamily(tagFamily);
	}

	public void removeProperty(String key) {
		boot.get().tagFamilyRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().tagFamilyRoot().inE(label, clazz);
	}

	public Project getProject() {
		return boot.get().tagFamilyRoot().getProject();
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().tagFamilyRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().tagFamilyRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().tagFamilyRoot().db();
	}

	public String getETag(TagFamily element, InternalActionContext ac) {
		return boot.get().tagFamilyRoot().getETag(element, ac);
	}

	public Vertx vertx() {
		return boot.get().tagFamilyRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().tagFamilyRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().tagFamilyRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().tagFamilyRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().tagFamilyRoot().setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		boot.get().tagFamilyRoot().setProperty(name, value);
	}

	public TraversalResult<? extends TagFamily> findAll() {
		return boot.get().tagFamilyRoot().findAll();
	}

	public Class<?> getTypeResolution() {
		return boot.get().tagFamilyRoot().getTypeResolution();
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().tagFamilyRoot().setTypeResolution(type);
	}

	public Stream<? extends TagFamily> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().tagFamilyRoot().findAllStream(ac, permission);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().tagFamilyRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().tagFamilyRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().tagFamilyRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().tagFamilyRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().tagFamilyRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().tagFamilyRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().tagFamilyRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends TagFamily> findAllDynamic() {
		return boot.get().tagFamilyRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().tagFamilyRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().tagFamilyRoot().outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().tagFamilyRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().linkOut(vertex, labels);
	}

	public TransformablePage<? extends TagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().tagFamilyRoot().findAll(ac, pagingInfo);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends TagFamily> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<TagFamily> extraFilter) {
		return boot.get().tagFamilyRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends TagFamily> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().tagFamilyRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagFamilyRoot().setLinkOut(vertex, labels);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().tagFamilyRoot().traversal();
	}

	public TagFamily findByName(String name) {
		return boot.get().tagFamilyRoot().findByName(name);
	}

	public JsonObject toJson() {
		return boot.get().tagFamilyRoot().toJson();
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().tagFamilyRoot().reframe(kind);
	}

	public TagFamily findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().tagFamilyRoot().findByName(ac, name, perm);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().tagFamilyRoot().reframeExplicit(kind);
	}

	public TagFamily findByUuid(String uuid) {
		return boot.get().tagFamilyRoot().findByUuid(uuid);
	}

	public TagFamily loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().tagFamilyRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public TagFamily loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().tagFamilyRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public TagFamily loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().tagFamilyRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().tagFamilyRoot().resolveToElement(stack);
	}

	public TagFamily create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().tagFamilyRoot().create(ac, batch);
	}

	public TagFamily create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().tagFamilyRoot().create(ac, batch, uuid);
	}

	public void addItem(TagFamily item) {
		boot.get().tagFamilyRoot().addItem(item);
	}

	public void removeItem(TagFamily item) {
		boot.get().tagFamilyRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().tagFamilyRoot().getRootLabel();
	}

	public Class<? extends TagFamily> getPersistanceClass() {
		return boot.get().tagFamilyRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().tagFamilyRoot().computeCount();
	}

	public PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid) {
		return boot.get().tagFamilyRoot().getRolePermissions(vertex, ac, roleUuid);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm) {
		return boot.get().tagFamilyRoot().getRolesWithPerm(vertex, perm);
	}

	public void delete(TagFamily element, BulkActionContext bac) {
		boot.get().tagFamilyRoot().delete(element, bac);
	}
	
	@Override
	public boolean update(TagFamily element, InternalActionContext ac, EventQueueBatch batch) {
		return element.getProject().getTagFamilyRoot().update(element, ac, batch);
	}

}
