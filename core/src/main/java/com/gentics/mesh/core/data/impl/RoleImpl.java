package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.util.VerticleHelper.processOrFail2;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.AbstractIndexedVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class RoleImpl extends AbstractIndexedVertex<RoleResponse>implements Role {

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
	public void addGroup(Group group) {
		setLinkOutTo(group.getImpl(), HAS_ROLE);
	}

	@Override
	public void grantPermissions(MeshVertex node, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {
			addFramedEdge(permission.label(), node.getImpl());
		}
	}

	@Override
	public Role transformToRest(ActionContext ac, Handler<AsyncResult<RoleResponse>> handler) {

		RoleResponse restRole = new RoleResponse();
		restRole.setName(getName());
		fillRest(restRole, ac);

		for (Group group : getGroups()) {
			GroupResponse restGroup = new GroupResponse();
			restGroup.setName(group.getName());
			restGroup.setUuid(group.getUuid());
			restRole.getGroups().add(restGroup);
		}

		handler.handle(Future.succeededFuture(restRole));
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
		getVertex().remove();
		addIndexBatch(DELETE_ACTION);
	}

	@Override
	public void update(ActionContext ac, Handler<AsyncResult<Void>> handler) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		if (!StringUtils.isEmpty(requestModel.getName()) && !getName().equals(requestModel.getName())) {
			if (boot.roleRoot().findByName(requestModel.getName()) != null) {
				ac.fail(CONFLICT, "role_conflicting_name");
				return;
			}
			SearchQueueBatch batch;
			try (Trx txUpdate = db.trx()) {
				setName(requestModel.getName());
				batch = addIndexBatch(UPDATE_ACTION);
				txUpdate.success();
			}
			processOrFail2(ac, batch, handler);
		}

	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		for (Group group : getGroups()) {
			batch.addEntry(group, SearchQueueEntryAction.UPDATE_ACTION);
		}
	}

}
