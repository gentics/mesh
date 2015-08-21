package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.json.JsonUtil.fromJson;
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
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RoleImpl extends AbstractGenericVertex<RoleResponse>implements Role {

	// TODO index on name

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
		return out(HAS_ROLE).has(RoleImpl.class).toListExplicit(GroupImpl.class);
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
	public Role transformToRest(RoutingContext rc, Handler<AsyncResult<RoleResponse>> handler) {

		RoleResponse restRole = new RoleResponse();
		restRole.setName(getName());
		fillRest(restRole, rc);

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
			// System.out.println(inE(permission.label()).mark().outV().retain(node).back().next().getLabel());
			outE(permission.label()).mark().inV().retain((MeshVertexImpl) node).back().removeAll();
			// System.out.println(outE(permission.label()).mark().inV().retain(node).back().next().getLabel());
		}
	}

	@Override
	public void delete() {
		getVertex().remove();
	}

	@Override
	public RoleImpl getImpl() {
		return this;
	}

	@Override
	public void update(RoutingContext rc) {
		RoleUpdateRequest requestModel = fromJson(rc, RoleUpdateRequest.class);
		I18NService i18n = I18NService.getI18n();
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		if (!StringUtils.isEmpty(requestModel.getName()) && !getName().equals(requestModel.getName())) {
			if (boot.roleRoot().findByName(requestModel.getName()) != null) {
				rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "role_conflicting_name")));
				return;
			}
			setName(requestModel.getName());
		}
	}

}
