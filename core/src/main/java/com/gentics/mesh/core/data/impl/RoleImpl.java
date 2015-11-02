package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.AbstractReferenceableCoreElement;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.RestModelHelper;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class RoleImpl extends AbstractReferenceableCoreElement<RoleResponse, RoleReference>implements Role {

	private static final Logger log = LoggerFactory.getLogger(RoleImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(RoleImpl.class);
	}
	
	@Override
	protected RoleReference createEmptyReferenceModel() {
		return new RoleReference();
	}

	@Override
	public String getType() {
		return Role.TYPE;
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public List<? extends Group> getGroups() {
		return out(HAS_ROLE).has(GroupImpl.class).toListExplicit(GroupImpl.class);
	}

	@Override
	public Set<GraphPermission> getPermissions(MeshVertex node) {
		Set<GraphPermission> permissions = new HashSet<>();
		Set<? extends String> labels = outE(GraphPermission.labels()).mark().inV().retain((MeshVertexImpl) node).back().label().toSet();
		for (String label : labels) {
			permissions.add(GraphPermission.valueOfLabel(label));
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(GraphPermission permission, GenericVertex<?> node) {
		return out(permission.label()).retain(node.getImpl()).hasNext();
	}

	@Override
	public void grantPermissions(MeshVertex node, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {

			boolean found = false;
			for (Edge edge : this.getElement().getEdges(Direction.OUT, permission.label())) {
				if (edge.getVertex(Direction.IN).getId().equals(node.getImpl().getId())) {
					found = true;

					if (log.isTraceEnabled()) {
						Vertex inV = edge.getVertex(Direction.IN);
						Vertex outV = edge.getVertex(Direction.OUT);
						log.trace("Found edge: " + edge.getLabel() + " from " + inV.getProperty("uuid") + ":"
								+ inV.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY) + " to " + outV.getProperty("uuid") + ":"
								+ outV.getProperty(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY) + ":" + outV.getProperty("name"));
					}
					break;
				}
			}
			if (!found) {
				addFramedEdge(permission.label(), node.getImpl());
			}
		}
	}

	@Override
	public Role transformToRest(InternalActionContext ac, Handler<AsyncResult<RoleResponse>> handler) {

		Database db = MeshSpringConfiguration.getInstance().database();
		db.asyncNoTrx(noTrx -> {
			Set<ObservableFuture<Void>> futures = new HashSet<>();

			RoleResponse restRole = new RoleResponse();
			restRole.setName(getName());

			for (Group group : getGroups()) {
				GroupResponse restGroup = new GroupResponse();
				restGroup.setName(group.getName());
				restGroup.setUuid(group.getUuid());
				restRole.getGroups().add(restGroup);
			}

			// Add common fields
			ObservableFuture<Void> obsFieldSet = RxHelper.observableFuture();
			futures.add(obsFieldSet);
			fillCommonRestFields(restRole, ac, rh -> {
				if (rh.failed()) {
					obsFieldSet.toHandler().handle(Future.failedFuture(rh.cause()));
				} else {
					obsFieldSet.toHandler().handle(Future.succeededFuture());
				}
			});

			// Role permissions
			RestModelHelper.setRolePermissions(ac, this, restRole);

			// Merge and complete
			Observable.merge(futures).last().subscribe(lastItem -> {
				noTrx.complete(restRole);
			} , error -> {
				noTrx.fail(error);
			});
		} , (AsyncResult<RoleResponse> rh) -> {
			handler.handle(rh);
		});
		return this;
	}

	@Override
	public void revokePermissions(MeshVertex node, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {
			outE(permission.label()).mark().inV().retain((MeshVertexImpl) node).back().removeAll();
		}
	}

	@Override
	public void delete() {
		addIndexBatch(DELETE_ACTION);
		getVertex().remove();
	}

	@Override
	public void update(InternalActionContext ac, Handler<AsyncResult<Void>> handler) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		Database db = MeshSpringConfiguration.getInstance().database();

		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		if (!StringUtils.isEmpty(requestModel.getName()) && !getName().equals(requestModel.getName())) {
			Role roleWithSameName = boot.roleRoot().findByName(requestModel.getName());
			if (roleWithSameName != null && !roleWithSameName.getUuid().equals(getUuid())) {
				HttpStatusCodeErrorException conflictError = conflict(ac, roleWithSameName.getUuid(), requestModel.getName(),
						"role_conflicting_name");
				handler.handle(Future.failedFuture(conflictError));
				return;
			}

			db.trx(tc -> {
				setName(requestModel.getName());
				SearchQueueBatch batch = addIndexBatch(UPDATE_ACTION);
				tc.complete(batch);
			} , (AsyncResult<SearchQueueBatch> rh) -> {
				if (rh.failed()) {
					handler.handle(Future.failedFuture(rh.cause()));
				} else {
					processOrFail2(ac, rh.result(), handler);
				}
			});
		}

	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		for (Group group : getGroups()) {
			batch.addEntry(group, SearchQueueEntryAction.UPDATE_ACTION);
		}
	}

}
