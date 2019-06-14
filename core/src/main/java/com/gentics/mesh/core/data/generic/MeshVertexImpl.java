package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NotImplementedException;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.madl.frame.AbstractVertexFrame;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

/**
 * @see MeshVertex
 */
@GraphElement
public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex {

	private Object id;
	private String uuid;

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(MeshVertexImpl.class, null);
		index.createIndex(vertexIndex(MeshVertexImpl.class)
			.withField("uuid", FieldType.STRING)
			.unique());
	}

	@Override
	protected void init() {
		super.init();
		property("uuid", UUIDUtil.randomUUID());
	}

	@Override
	protected void init(FramedGraph graph, Element element) {
		super.init(graph, null);
		this.id = element.getId();
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

	@SuppressWarnings("unchecked")
	public Object getId() {
		return id;
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
		return property(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY);
	}

	@Override
	public FramedGraph getGraph() {
		return Tx.getActive().getGraph();
	}

	@Override
	public void delete(BulkActionContext context) {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
	}

	@Override
	public void applyPermissions(EventQueueBatch batch, Role role, boolean recursive, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		applyVertexPermissions(batch, role, permissionsToGrant, permissionsToRevoke);
	}

	protected void applyVertexPermissions(EventQueueBatch batch, Role role, Set<GraphPermission> permissionsToGrant,
		Set<GraphPermission> permissionsToRevoke) {
		role.grantPermissions(this, permissionsToGrant.toArray(new GraphPermission[permissionsToGrant.size()]));
		role.revokePermissions(this, permissionsToRevoke.toArray(new GraphPermission[permissionsToRevoke.size()]));

		if (this instanceof MeshCoreVertex) {
			MeshCoreVertex<?, ?> coreVertex = (MeshCoreVertex<?, ?>) this;
			batch.add(coreVertex.onPermissionChanged(role));
		}
		// TODO Also handle RootVertex - We need to add a dedicated event in those cases.
	}

	@Override
	public Vertex getElement() {
		// TODO FIXME We should store the element reference in a thread local map that is bound to the transaction. The references should be removed once the
		// transaction finishes
		FramedGraph fg = Tx.getActive().getGraph();
		if (fg == null) {
			throw new RuntimeException(
				"Could not find thread local graph. The code is most likely not being executed in the scope of a transaction.");
		}

		Vertex vertexForId = fg.getVertex(id);
		if (vertexForId == null) {
			throw new RuntimeException("No vertex for Id {" + id + "} of type {" + getClass().getName() + "} could be found within the graph");
		}
		Element vertex = ((WrappedVertex) vertexForId).getBaseElement();

		// Unwrap wrapped vertex
		if (vertex instanceof WrappedElement) {
			vertex = (Vertex) ((WrappedElement) vertex).getBaseElement();
		}
		return (Vertex) vertex;
	}

	@Override
	public String getElementVersion() {
		Vertex vertex = getElement();
		return MeshInternal.get().database().getElementVersion(vertex);
	}


}
