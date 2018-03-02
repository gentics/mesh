package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Set;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.dagger.MeshInternal;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract class for mesh core vertices that includes methods which are commonly used when transforming the vertices into REST POJO's.
 * 
 * @param <T>
 *            Rest model representation of the core vertex
 * @param <R>
 *            Type of the core vertex which is used to determine type of chained vertices
 */
public abstract class AbstractMeshCoreVertex<T extends RestModel, R extends MeshCoreVertex<T, R>> extends MeshVertexImpl
	implements MeshCoreVertex<T, R> {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshCoreVertex.class);

	@Override
	public Iterable<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return in(perm.label()).frameExplicit(RoleImpl.class);
	}

	@Override
	public void setRolePermissions(InternalActionContext ac, GenericRestResponse model) {
		String roleUuid = ac.getRolePermissionParameters().getRoleUuid();
		if (!isEmpty(roleUuid)) {
			Role role = MeshInternal.get().boot().meshRoot().getRoleRoot().loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {

				PermissionInfo permissionInfo = new PermissionInfo();
				Set<GraphPermission> permSet = role.getPermissions(this);
				for (GraphPermission permission : permSet) {
					permissionInfo.set(permission.getRestPerm(), true);
				}
				permissionInfo.setOthers(false);
				model.setRolePerms(permissionInfo);
			}
		}
	}

	@Override
	public void fillCommonRestFields(InternalActionContext ac, GenericRestResponse model) {
		model.setUuid(getUuid());

		if (this instanceof EditorTrackingVertex) {
			EditorTrackingVertex edited = (EditorTrackingVertex) this;

			User editor = edited.getEditor();
			if (editor != null) {
				model.setEditor(editor.transformToReference());
			} else {
				log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no editor. Omitting editor field");
			}

			String date = edited.getLastEditedDate();
			model.setEdited(date);
		}

		if (this instanceof CreatorTrackingVertex) {
			CreatorTrackingVertex created = (CreatorTrackingVertex) this;
			User creator = created.getCreator();
			if (creator != null) {
				model.setCreator(creator.transformToReference());
			}
			String date = created.getCreationDate();
			model.setCreated(date);
		}

		// When this is a node migration, do not set user permissions
		if (!(ac instanceof NodeMigrationActionContextImpl)) {
			PermissionInfo permissionInfo = ac.getUser().getPermissionInfo(this);
			model.setPermissions(permissionInfo);
		}
	}

	/**
	 * Compare both string values in order to determine whether the graph value should be updated.
	 * 
	 * @param restValue
	 *            Rest model string value
	 * @param graphValue
	 *            Graph string value
	 * @return true if restValue is not empty or null and the restValue is not equal to the graph value. Otherwise false.
	 */
	protected boolean shouldUpdate(String restValue, String graphValue) {
		return !isEmpty(restValue) && !restValue.equals(graphValue);
	}

	@Override
	public void onUpdated() {
		String address = getTypeInfo().getOnUpdatedAddress();
		if (address != null) {
			JsonObject json = new JsonObject();
			if (this instanceof NamedElement) {
				json.put("name", ((NamedElement) this).getName());
			}
			json.put("origin", Mesh.mesh().getOptions().getNodeName());
			json.put("uuid", getUuid());
			Mesh.vertx().eventBus().publish(address, json);
			if (log.isDebugEnabled()) {
				log.debug("Updated event sent {" + address + "}");
			}
		}
	}

	@Override
	public void onCreated() {
		String address = getTypeInfo().getOnCreatedAddress();
		if (address != null) {
			JsonObject json = new JsonObject();
			if (this instanceof NamedElement) {
				json.put("name", ((NamedElement) this).getName());
			}
			json.put("origin", Mesh.mesh().getOptions().getNodeName());
			json.put("uuid", getUuid());
			Mesh.vertx().eventBus().publish(address, json);
			if (log.isDebugEnabled()) {
				log.debug("Created event sent {" + address + "}");
			}
		}
	}

	@Override
	public void onDeleted(String uuid, String name) {
		String address = getTypeInfo().getOnDeletedAddress();
		if (address != null) {
			JsonObject json = new JsonObject();
			if (this instanceof NamedElement) {
				json.put("name", name);
			}
			json.put("origin", Mesh.mesh().getOptions().getNodeName());
			json.put("uuid", getUuid());
			Mesh.vertx().eventBus().publish(address, json);
			if (log.isDebugEnabled()) {
				log.debug("Deleted event sent {" + address + "}");
			}
		}
	}

	@Override
	public String getETag(InternalActionContext ac) {
		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(getUuid());
		keyBuilder.append("-");
		keyBuilder.append(ac.getUser().getPermissionInfo(this).getHash());
		return keyBuilder.toString();
	}

}
