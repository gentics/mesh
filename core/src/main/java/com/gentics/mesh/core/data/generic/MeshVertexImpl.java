package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.AbstractVertexFrame;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;

/**
 * @see MeshVertex
 */
@GraphElement
public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex, HibBaseElement {

	private String uuid;

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MeshVertexImpl.class, null);
		index.createIndex(vertexIndex(MeshVertexImpl.class)
			.withField("uuid", FieldType.STRING)
			.unique());
	}

	@Override
	protected void init(FramedGraph graph, Element element, Object id) {
		super.init(graph, null, id);
	}

	@Override
	protected void init() {
		super.init();
		property("uuid", UUIDUtil.randomUUID());
	}

	/**
	 * Return the properties which are prefixed using the given key.
	 * 
	 * @param prefix
	 *            Property prefix
	 * @return Found properties
	 */
	public <T> Map<String, T> getProperties(String prefix) {
		Map<String, T> properties = new HashMap<>();

		for (String key : getPropertyKeys()) {
			if (key.startsWith(prefix)) {
				properties.put(key, getProperty(key));
			}
		}
		return properties;
	}

	public String getUuid() {
		// Return the locally stored uuid if possible. Otherwise load it from the graph.
		if (uuid == null) {
			this.uuid = property("uuid");
		}
		return uuid;
	}

	public void setUuid(String uuid) {
		setProperty("uuid", uuid);
		this.uuid = uuid;
	}

	public Vertex getVertex() {
		return getElement();
	}

	public String getFermaType() {
		return property(TYPE_RESOLUTION_KEY);
	}

	@Override
	public FramedGraph getGraph() {
		return Tx.get().getGraph();
	}

	@Override
	public void delete(BulkActionContext context) {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		applyVertexPermissions(batch, role, permissionsToGrant, permissionsToRevoke);
	}

	protected void applyVertexPermissions(EventQueueBatch batch, Role role, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		RoleDaoWrapper roleDao = mesh().boot().roleDao();
		roleDao.grantPermissions(role, this, permissionsToGrant.toArray(new InternalPermission[permissionsToGrant.size()]));
		roleDao.revokePermissions(role, this, permissionsToRevoke.toArray(new InternalPermission[permissionsToRevoke.size()]));

		if (this instanceof MeshCoreVertex) {
			MeshCoreVertex<?> coreVertex = (MeshCoreVertex<?>) this;
			batch.add(coreVertex.onPermissionChanged(role));
		}
		// TODO Also handle RootVertex - We need to add a dedicated event in those cases.
	}

	@Override
	public String getElementVersion() {
		Vertex vertex = getElement();
		return mesh().database().getElementVersion(vertex);
	}

	@Override
	public void setCachedUuid(String uuid) {
		this.uuid = uuid;
	}

	public MeshComponent mesh() {
		return getGraphAttribute(GraphAttribute.MESH_COMPONENT);
	}

	public Mesh meshApi() {
		return mesh().boot().mesh();
	}

	public MeshOptions options() {
		return mesh().options();
	}

	@Override
	public Database db() {
		return mesh().database();
	}

	@Override
	public Vertx vertx() {
		return mesh().vertx();
	}

	public EventQueueBatch createBatch() {
		return mesh().batchProvider().get();
	}

	/**
	 * Return the used vertx (rx variant) instance for mesh.
	 * 
	 * @return Rx Vertx instance
	 */
	public io.vertx.reactivex.core.Vertx rxVertx() {
		return new io.vertx.reactivex.core.Vertx(vertx());
	}

	@Override
	public Set<String> getRoleUuidsForPerm(InternalPermission permission) {
		Set<String> oset = property(permission.propertyKey());
		if (oset == null) {
			return new HashSet<>(10);
		} else {
			return new HashSet<>(oset);
		}
	}

	@Override
	public void setRoleUuidForPerm(InternalPermission permission, Set<String> allowedRoles) {
		property(permission.propertyKey(), allowedRoles);
	}

}
