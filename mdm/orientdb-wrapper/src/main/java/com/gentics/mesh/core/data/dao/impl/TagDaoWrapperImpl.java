package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.apache.commons.lang3.StringUtils.isEmpty;

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
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
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

public class TagDaoWrapperImpl implements TagDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public TagDaoWrapperImpl(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	public Object id() {
		return boot.get().tagRoot().id();
	}

	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return boot.get().tagRoot().getRolePermissions(ac, roleUuid);
	}

	public void addTag(Tag tag) {
		boot.get().tagRoot().addTag(tag);
	}

	public void setUuid(String uuid) {
		boot.get().tagRoot().setUuid(uuid);
	}

	public void setUniqueLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().tagRoot().setUniqueLinkOutTo(vertex, labels);
	}

	public void removeTag(Tag tag) {
		boot.get().tagRoot().removeTag(tag);
	}

	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return boot.get().tagRoot().getRolesWithPerm(perm);
	}

	public String getUuid() {
		return boot.get().tagRoot().getUuid();
	}

	public Tag create(String name, Project project, TagFamily tagFamily, User creator) {
		return boot.get().tagRoot().create(name, project, tagFamily, creator);
	}

	public Vertex getVertex() {
		return boot.get().tagRoot().getVertex();
	}

	public String getElementVersion() {
		return boot.get().tagRoot().getElementVersion();
	}

	public void setUniqueLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().tagRoot().setUniqueLinkInTo(vertex, labels);
	}

	public <T> T property(String name) {
		return boot.get().tagRoot().property(name);
	}

	public void delete(BulkActionContext bac) {
		boot.get().tagRoot().delete(bac);
	}

	public Vertex getElement() {
		return boot.get().tagRoot().getElement();
	}

	public void setSingleLinkOutTo(VertexFrame vertex, String... labels) {
		boot.get().tagRoot().setSingleLinkOutTo(vertex, labels);
	}

	public Object getId() {
		return boot.get().tagRoot().getId();
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().tagRoot().addFramedEdge(label, inVertex, initializer);
	}

	public void setSingleLinkInTo(VertexFrame vertex, String... labels) {
		boot.get().tagRoot().setSingleLinkInTo(vertex, labels);
	}

	public Set<String> getPropertyKeys() {
		return boot.get().tagRoot().getPropertyKeys();
	}

	public void addToStringSetProperty(String propertyKey, String value) {
		boot.get().tagRoot().addToStringSetProperty(propertyKey, value);
	}

	public VertexTraversal<?, ?, ?> out(String... labels) {
		return boot.get().tagRoot().out(labels);
	}

	public void remove() {
		boot.get().tagRoot().remove();
	}

	public void delete() {
		boot.get().tagRoot().delete();
	}

	public <T extends ElementFrame> TraversalResult<? extends T> out(String label, Class<T> clazz) {
		return boot.get().tagRoot().out(label, clazz);
	}

	public FramedGraph getGraph() {
		return boot.get().tagRoot().getGraph();
	}

	public <R> void property(String key, R value) {
		boot.get().tagRoot().property(key, value);
	}

	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		boot.get().tagRoot().applyPermissions(batch, role, recursive, permissionsToGrant, permissionsToRevoke);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> outE(String label, Class<T> clazz) {
		return boot.get().tagRoot().outE(label, clazz);
	}

	public <T> T getProperty(String name) {
		return boot.get().tagRoot().getProperty(name);
	}

	public <T extends ElementFrame> TraversalResult<? extends T> in(String label, Class<T> clazz) {
		return boot.get().tagRoot().in(label, clazz);
	}

	public <T> T addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().tagRoot().addFramedEdge(label, inVertex, kind);
	}

	public void removeProperty(String key) {
		boot.get().tagRoot().removeProperty(key);
	}

	public <T extends EdgeFrame> TraversalResult<? extends T> inE(String label, Class<T> clazz) {
		return boot.get().tagRoot().inE(label, clazz);
	}

	public <T extends RawTraversalResult<?>> T traverse(Function<GraphTraversal<Vertex, Vertex>, GraphTraversal<?, ?>> traverser) {
		return boot.get().tagRoot().traverse(traverser);
	}

	public <T> T getProperty(String name, Class<T> type) {
		return boot.get().tagRoot().getProperty(name, type);
	}

	public Database db() {
		return boot.get().tagRoot().db();
	}

	public Vertx vertx() {
		return boot.get().tagRoot().vertx();
	}

	public boolean hasPublishPermissions() {
		return boot.get().tagRoot().hasPublishPermissions();
	}

	public MeshOptions options() {
		return boot.get().tagRoot().options();
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, ClassInitializer<T> initializer) {
		return boot.get().tagRoot().addFramedEdgeExplicit(label, inVertex, initializer);
	}

	public void setCachedUuid(String uuid) {
		boot.get().tagRoot().setCachedUuid(uuid);
	}

	public TraversalResult<? extends Tag> findAll() {
		return boot.get().tagRoot().findAll();
	}

	public void setProperty(String name, Object value) {
		boot.get().tagRoot().setProperty(name, value);
	}

	public Class<?> getTypeResolution() {
		return boot.get().tagRoot().getTypeResolution();
	}

	public Stream<? extends Tag> findAllStream(InternalActionContext ac, GraphPermission permission) {
		return boot.get().tagRoot().findAllStream(ac, permission);
	}

	public void setTypeResolution(Class<?> type) {
		boot.get().tagRoot().setTypeResolution(type);
	}

	public <T> T addFramedEdgeExplicit(String label, com.syncleus.ferma.VertexFrame inVertex, Class<T> kind) {
		return boot.get().tagRoot().addFramedEdgeExplicit(label, inVertex, kind);
	}

	public void removeTypeResolution() {
		boot.get().tagRoot().removeTypeResolution();
	}

	public VertexTraversal<?, ?, ?> v() {
		return boot.get().tagRoot().v();
	}

	public EdgeTraversal<?, ?, ?> e() {
		return boot.get().tagRoot().e();
	}

	public EdgeTraversal<?, ?, ?> e(Object... ids) {
		return boot.get().tagRoot().e(ids);
	}

	public TEdge addFramedEdge(String label, com.syncleus.ferma.VertexFrame inVertex) {
		return boot.get().tagRoot().addFramedEdge(label, inVertex);
	}

	public <T> T getGraphAttribute(String key) {
		return boot.get().tagRoot().getGraphAttribute(key);
	}

	public TraversalResult<? extends Tag> findAllDynamic() {
		return boot.get().tagRoot().findAllDynamic();
	}

	public VertexTraversal<?, ?, ?> in(String... labels) {
		return boot.get().tagRoot().in(labels);
	}

	public EdgeTraversal<?, ?, ?> outE(String... labels) {
		return boot.get().tagRoot().outE(labels);
	}

	public TransformablePage<? extends Tag> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().tagRoot().findAll(ac, pagingInfo);
	}

	public EdgeTraversal<?, ?, ?> inE(String... labels) {
		return boot.get().tagRoot().inE(labels);
	}

	public void linkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagRoot().linkOut(vertex, labels);
	}

	public void linkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagRoot().linkIn(vertex, labels);
	}

	public TransformablePage<? extends Tag> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Tag> extraFilter) {
		return boot.get().tagRoot().findAll(ac, pagingInfo, extraFilter);
	}

	public void unlinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagRoot().unlinkOut(vertex, labels);
	}

	public void unlinkIn(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagRoot().unlinkIn(vertex, labels);
	}

	public TransformablePage<? extends Tag> findAllNoPerm(InternalActionContext ac, PagingParameters pagingInfo) {
		return boot.get().tagRoot().findAllNoPerm(ac, pagingInfo);
	}

	public void setLinkOut(com.syncleus.ferma.VertexFrame vertex, String... labels) {
		boot.get().tagRoot().setLinkOut(vertex, labels);
	}

	public Tag findByName(String name) {
		return boot.get().tagRoot().findByName(name);
	}

	public VertexTraversal<?, ?, ?> traversal() {
		return boot.get().tagRoot().traversal();
	}

	public JsonObject toJson() {
		return boot.get().tagRoot().toJson();
	}

	public Tag findByName(InternalActionContext ac, String name, GraphPermission perm) {
		return boot.get().tagRoot().findByName(ac, name, perm);
	}

	public <T> T reframe(Class<T> kind) {
		return boot.get().tagRoot().reframe(kind);
	}

	public <T> T reframeExplicit(Class<T> kind) {
		return boot.get().tagRoot().reframeExplicit(kind);
	}

	public Tag findByUuid(String uuid) {
		return boot.get().tagRoot().findByUuid(uuid);
	}

	public Tag loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		return boot.get().tagRoot().loadObjectByUuid(ac, uuid, perm);
	}

	public Tag loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		return boot.get().tagRoot().loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	public Tag loadObjectByUuidNoPerm(String uuid, boolean errorIfNotFound) {
		return boot.get().tagRoot().loadObjectByUuidNoPerm(uuid, errorIfNotFound);
	}

	public MeshVertex resolveToElement(Stack<String> stack) {
		return boot.get().tagRoot().resolveToElement(stack);
	}

	public Tag create(InternalActionContext ac, EventQueueBatch batch) {
		return boot.get().tagRoot().create(ac, batch);
	}

	public Tag create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		return boot.get().tagRoot().create(ac, batch, uuid);
	}

	public void addItem(Tag item) {
		boot.get().tagRoot().addItem(item);
	}

	public void removeItem(Tag item) {
		boot.get().tagRoot().removeItem(item);
	}

	public String getRootLabel() {
		return boot.get().tagRoot().getRootLabel();
	}

	public Class<? extends Tag> getPersistanceClass() {
		return boot.get().tagRoot().getPersistanceClass();
	}

	public long computeCount() {
		return boot.get().tagRoot().computeCount();
	}

	@Override
	public TagResponse transformToRestSync(Tag tag, InternalActionContext ac, int level, String... languageTags) {
		return boot.get().tagRoot().transformToRestSync(tag, ac, level, languageTags);
	}

	@Override
	public String getAPIPath(Tag tag, InternalActionContext ac) {
		return boot.get().tagRoot().getAPIPath(tag, ac);
	}

	@Override
	public String getETag(Tag tag, InternalActionContext ac) {
		return boot.get().tagRoot().getETag(tag, ac);
	}

	// New Methods

	@Override
	public TraversalResult<? extends Tag> findAll(TagFamily tagFamily) {
		return tagFamily.findAll();
	}

	@Override
	public String getSubETag(Tag tag, InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(tag.getLastEditedTimestamp());
		keyBuilder.append(ac.getBranch(tag.getProject()).getUuid());
		return keyBuilder.toString();
	}

	@Override
	public boolean update(Tag tag, InternalActionContext ac, EventQueueBatch batch) {
		TagUpdateRequest requestModel = ac.fromJson(TagUpdateRequest.class);
		String newTagName = requestModel.getName();
		if (isEmpty(newTagName)) {
			throw error(BAD_REQUEST, "tag_name_not_set");
		} else {
			TagFamily tagFamily = tag.getTagFamily();

			// Check for conflicts
			Tag foundTagWithSameName = tagFamily.findByName(newTagName);
			if (foundTagWithSameName != null && !foundTagWithSameName.getUuid().equals(getUuid())) {
				throw conflict(foundTagWithSameName.getUuid(), newTagName, "tag_create_tag_with_same_name_already_exists", newTagName, tagFamily
					.getName());
			}

			if (!newTagName.equals(tag.getName())) {
				tag.setEditor(ac.getUser());
				tag.setLastEditedTimestamp();
				tag.setName(newTagName);
				batch.add(tag.onUpdated());
				return true;
			}
		}
		return false;
	}

	@Override
	public void delete(Tag tag, BulkActionContext bac) {
		boot.get().tagRoot().delete(tag, bac);
	}

}
