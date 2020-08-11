package com.gentics.mesh.core.data.dao.impl;

import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.madl.traversal.RawTraversalResult;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
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
import io.vertx.core.Vertx;

@Singleton
public class LanguageDaoWrapperImpl extends AbstractDaoWrapper implements LanguageDaoWrapper {

	@Inject
	public LanguageDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		super(boot, permissions);
	}

	public Object id() {
		return boot.get().languageRoot().id();
	}

	public Language create(String languageName, String languageTag) {
		return boot.get().languageRoot().create(languageName, languageTag);
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().languageRoot().getRolePermissions(ac, roleUuid);
	}

	public void setUuid(String uuid) {
		boot.get().languageRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().languageRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public Language create(String languageName, String languageTag, String uuid) {
		return boot.get().languageRoot().create(languageName, languageTag, uuid);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().languageRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().languageRoot().getUuid();
	}

	public void addLanguage(Language language) {
		boot.get().languageRoot().addLanguage(language);
	}

	public Vertex getVertex() {
		return boot.get().languageRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().languageRoot().getElementVersion();
	}

	public Language findByLanguageTag(String languageTag) {
		return boot.get().languageRoot().findByLanguageTag(languageTag);
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().languageRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().languageRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().languageRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().languageRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().languageRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().languageRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().languageRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().languageRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().languageRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().languageRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().languageRoot().out(labels);
	}

	public void remove() {
		boot.get().languageRoot().remove();
	}

	public void delete() {
		boot.get().languageRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().languageRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().languageRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().languageRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().languageRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().languageRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().languageRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().languageRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().languageRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().languageRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().languageRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().languageRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().languageRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().languageRoot().db();
	}

	public Vertx vertx() {
		return boot.get().languageRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().languageRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().languageRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().languageRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().languageRoot().setCachedUuid(uuid);
	}

	public void setProperty(String name, Object value) {
		boot.get().languageRoot().setProperty(name, value);
	}

	public TraversalResult<? extends Language> findAll() {
		return boot.get().languageRoot().findAll();
	}

	public Class<?> getTypeResolution() {
		return boot.get().languageRoot().getTypeResolution();
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().languageRoot().setTypeResolution(type);
	}

	public Stream<? extends Language> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().languageRoot().findAllStream(ac, permission);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().languageRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().languageRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().languageRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().languageRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().languageRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().languageRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().languageRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends Language> findAllDynamic() {
		return boot.get().languageRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().languageRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().languageRoot().outE(labels);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().languageRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().languageRoot().linkOut(vertex, labels);
	}

	public TransformablePage<? extends Language> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().languageRoot().findAll(ac, pagingInfo);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().languageRoot().linkIn(vertex, labels);
	}

	public Page<? extends Language> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Language> extraFilter) {
		return boot.get().languageRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().languageRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().languageRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends Language> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().languageRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().languageRoot().setLinkOut(vertex, labels);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().languageRoot().traversal();
	}

	public Language findByName(String name) {
		return boot.get().languageRoot().findByName(name);
	}

	public JsonObject toJson() {
		return boot.get().languageRoot().toJson();
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().languageRoot().reframe(kind);
	}

	public Language findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().languageRoot().findByName(ac, name, perm);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().languageRoot().reframeExplicit(kind);
	}

	public Language findByUuid(String uuid) {
		return boot.get().languageRoot().findByUuid(uuid);
	}

	public Language loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().languageRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Language loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().languageRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Language loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().languageRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public HibElement resolveToElement(Stack<String> stack) {
		return boot.get().languageRoot().resolveToElement(stack);
	}

	public Language create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().languageRoot().create(ac, batch);
	}

	public Language create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().languageRoot().create(ac, batch, uuid);
	}

	public void addItem(Language item) {
		boot.get().languageRoot().addItem(item);
	}

	public void removeItem(Language item) {
		boot.get().languageRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().languageRoot().getRootLabel();
	}

	public Class<? extends Language> getPersistanceClass() {
		return boot.get().languageRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().languageRoot().computeCount();
	}

	public PermissionInfo getRolePermissions(MeshVertex vertex, InternalActionContext ac, String roleUuid) {
		return boot.get().languageRoot().getRolePermissions(vertex, ac, roleUuid);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(MeshVertex vertex, GraphPermission perm) {
		return boot.get().languageRoot().getRolesWithPerm(vertex, perm);
	}

	public void delete(Language element, BulkActionContext bac) {
		boot.get().languageRoot().delete(element, bac);
	}

}
