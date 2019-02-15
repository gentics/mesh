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
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.impl.MeshEventModelImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.parameter.value.FieldsSet;

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
	public TraversalResult<? extends Role> getRolesWithPerm(GraphPermission perm) {
		return new TraversalResult<>(in(perm.label()).frameExplicit(RoleImpl.class));
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
	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		if (fields.has("uuid")) {
			model.setUuid(getUuid());
		}

		if (this instanceof EditorTrackingVertex) {
			EditorTrackingVertex edited = (EditorTrackingVertex) this;

			if (fields.has("editor")) {
				User editor = edited.getEditor();
				if (editor != null) {
					model.setEditor(editor.transformToReference());
				} else {
					log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no editor. Omitting editor field");
				}
			}

			if (fields.has("edited")) {
				String date = edited.getLastEditedDate();
				model.setEdited(date);
			}
		}

		if (this instanceof CreatorTrackingVertex) {
			CreatorTrackingVertex created = (CreatorTrackingVertex) this;
			if (fields.has("creator")) {
				User creator = created.getCreator();
				if (creator != null) {
					model.setCreator(creator.transformToReference());
				}
			}
			if (fields.has("created")) {
				String date = created.getCreationDate();
				model.setCreated(date);
			}
		}

		if (fields.has("perms")) {
			// When this is a node migration, do not set user permissions
			if (!(ac instanceof NodeMigrationActionContextImpl)) {
				PermissionInfo permissionInfo = ac.getUser().getPermissionInfo(this);
				model.setPermissions(permissionInfo);
			}
		}
	}

	/**
	 * Compare both string values in order to determine whether the graph value should be updated.
	 * 
	 * @param restValue
	 *            Rest model string value
	 * @param graphValue
	 *            Graph string value
	 * @return true if restValue is not null and the restValue is not equal to the graph value. Otherwise false.
	 */
	protected boolean shouldUpdate(String restValue, String graphValue) {
		return restValue != null && !restValue.equals(graphValue);
	}

	@Override
	public MeshElementEventModel onUpdated() {
		MeshElementEventModel event = new MeshEventModelImpl();
		event.setEvent(getTypeInfo().getOnUpdated());
		fillEventInfo(event);
		return event;
	}

	@Override
	public MeshElementEventModel onCreated() {
		MeshElementEventModel event = new MeshEventModelImpl();
		event.setEvent(getTypeInfo().getOnCreated());
		fillEventInfo(event);
		return event;
	}

	@Override
	public MeshElementEventModel onDeleted() {
		MeshElementEventModel event = new MeshEventModelImpl();
		event.setEvent(getTypeInfo().getOnDeleted());
		fillEventInfo(event);
		return event;
	}

	protected void fillEventInfo(MeshElementEventModel event) {
		if (this instanceof NamedElement) {
			event.setName(((NamedElement) this).getName());
		}
		event.setOrigin(Mesh.mesh().getOptions().getNodeName());
		event.setUuid(getUuid());
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
