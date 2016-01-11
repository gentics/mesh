package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.UPDATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

/**
 * @see Role
 */
public class RoleImpl extends AbstractMeshCoreVertex<RoleResponse, Role> implements Role {

	private static final Logger log = LoggerFactory.getLogger(RoleImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(RoleImpl.class);
	}

	@Override
	public RoleReference createEmptyReferenceModel() {
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

	/**
	 * @deprecated Use {@link #getPermissions(MeshVertex)} instead.
	 */
	@Override
	@Deprecated
	public boolean hasPermission(GraphPermission permission, MeshVertex vertex) {
		return out(permission.label()).retain(vertex.getImpl()).hasNext();
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
	public Observable<RoleResponse> transformToRest(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();
		return db.asyncNoTrxExperimental(() -> {
			Set<Observable<RoleResponse>> obs = new HashSet<>();

			RoleResponse restRole = new RoleResponse();
			restRole.setName(getName());

			for (Group group : getGroups()) {
				restRole.getGroups().add(group.transformToReference(ac));
			}

			// Add common fields
			obs.add(fillCommonRestFields(ac, restRole));

			// Role permissions
			obs.add(setRolePermissions(ac, restRole));

			// Merge and complete
			return Observable.merge(obs).last();
		});
	}

	@Override
	public void revokePermissions(MeshVertex node, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {
			outE(permission.label()).mark().inV().retain((MeshVertexImpl) node).back().removeAll();
		}
	}

	@Override
	public void delete() {
		// TODO don't allow deletion of admin role
		addIndexBatch(DELETE_ACTION);
		getVertex().remove();
	}

	@Override
	public Observable<? extends Role> update(InternalActionContext ac) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		Database db = MeshSpringConfiguration.getInstance().database();

		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		if (!StringUtils.isEmpty(requestModel.getName()) && !getName().equals(requestModel.getName())) {
			Role roleWithSameName = boot.roleRoot().findByName(requestModel.getName()).toBlocking().single();
			if (roleWithSameName != null && !roleWithSameName.getUuid().equals(getUuid())) {
				throw conflict(roleWithSameName.getUuid(), requestModel.getName(), "role_conflicting_name");
			}

			return db.trx(() -> {
				setName(requestModel.getName());
				return addIndexBatch(UPDATE_ACTION);
			}).process().map(b -> this);
		}
		// No update required
		return Observable.just(this);
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		for (Group group : getGroups()) {
			batch.addEntry(group, SearchQueueEntryAction.UPDATE_ACTION);
		}
	}

}
