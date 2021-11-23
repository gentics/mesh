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
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.AbstractVertexFrame;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.graph.GraphAttribute;
import com.gentics.mesh.dagger.OrientDBMeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.madl.field.FieldType;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.FramedGraph;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Vertx;

/**BranchRootImpl
 * @see MeshVertex
 */
@GraphElement
public class MeshVertexImpl extends AbstractVertexFrame implements MeshVertex, HibBaseElement {

	private String uuid;

	/**
	 * Initialize the vertex type and index.
	 */
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

	@Getter
	public String getUuid() {
		// Return the locally stored uuid if possible. Otherwise load it from the graph.
		if (uuid == null) {
			this.uuid = property("uuid");
		}
		return uuid;
	}

	@Setter
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
		return GraphDBTx.getGraphTx().getGraph();
	}

	@Override
	public void delete(BulkActionContext context) {
		throw new NotImplementedException("The deletion behaviour for this vertex was not implemented.");
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

	/**
	 * Return the dagger mesh context from the graph attributes. The component is accessed this way since it is not otherwise possible to inject dagger into
	 * domain classes.
	 * 
	 * @return
	 */
	public OrientDBMeshComponent mesh() {
		return getGraphAttribute(GraphAttribute.MESH_COMPONENT);
	}

	/**
	 * Return the public mesh API
	 * 
	 * @return
	 */
	public Mesh meshApi() {
		return mesh().boot().mesh();
	}

	/**
	 * Return the Mesh options.
	 */
	public MeshOptions options() {
		return mesh().options();
	}

	@Override
	public GraphDatabase db() {
		return mesh().database();
	}

	@Override
	public Vertx vertx() {
		return mesh().vertx();
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
