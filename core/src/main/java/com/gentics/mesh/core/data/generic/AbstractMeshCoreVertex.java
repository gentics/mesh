package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.NamedElement;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.result.Result;
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
public abstract class AbstractMeshCoreVertex<T extends RestModel> extends MeshVertexImpl
	implements MeshCoreVertex<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshCoreVertex.class);

	@Override
	public PermissionInfo getRolePermissions(InternalActionContext ac, String roleUuid) {
		return mesh().permissionProperties().getRolePermissions(this, ac, roleUuid);
	}

	@Override
	public Result<? extends HibRole> getRolesWithPerm(InternalPermission perm) {
		return mesh().permissionProperties().getRolesWithPerm(this, perm);
	}

	/**
	 * Set the role permissions to the REST model.
	 * 
	 * @param ac
	 * @param model
	 */
	public void setRolePermissions(InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(getRolePermissions(ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	@Override
	public void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		if (fields.has("uuid")) {
			model.setUuid(getUuid());
		}

		if (this instanceof EditorTrackingVertex) {
			EditorTrackingVertex edited = (EditorTrackingVertex) this;

			if (fields.has("editor")) {
				HibUser editor = edited.getEditor();
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
				HibUser creator = created.getCreator();
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
				PermissionInfo permissionInfo = mesh().boot().userDao().getPermissionInfo(ac.getUser(), this);
				model.setPermissions(permissionInfo);
			}
		}
	}

	/**
	 * Compare both values in order to determine whether the graph value should be updated.
	 * 
	 * @param restValue
	 *            Rest model string value
	 * @param graphValue
	 *            Graph string value
	 * @return true if restValue is not null and the restValue is not equal to the graph value. Otherwise false.
	 * @deprecated This method was moved to AbstractDaoWrapper
	 */
	@Deprecated
	protected <T> boolean shouldUpdate(T restValue, T graphValue) {
		return restValue != null && !restValue.equals(graphValue);
	}

	@Override
	public MeshElementEventModel createEvent(MeshEvent event) {
		MeshElementEventModel model = new MeshElementEventModelImpl();
		model.setEvent(event);
		fillEventInfo(model);
		return model;
	}

	@Override
	public void fillEventInfo(MeshElementEventModel model) {
		if (this instanceof NamedElement) {
			model.setName(((NamedElement) this).getName());
		}
		model.setOrigin(options().getNodeName());
		model.setUuid(getUuid());
	}

	@Override
	public PermissionChangedEventModelImpl onPermissionChanged(Role role) {
		PermissionChangedEventModelImpl model = new PermissionChangedEventModelImpl();
		fillPermissionChanged(model, role);
		return model;
	}

	@Override
	public void fillPermissionChanged(PermissionChangedEventModelImpl model, Role role) {
		model.setEvent(ROLE_PERMISSIONS_CHANGED);
		model.setRole(role.transformToReference());
		model.setType(getTypeInfo().getType());
		model.setUuid(getUuid());
		if (this instanceof NamedElement) {
			String name = ((NamedElement) this).getName();
			model.setName(name);
		}
		if (this instanceof ProjectElement) {
			HibProject project = ((ProjectElement) this).getProject();
			if (project != null) {
				if (model instanceof PermissionChangedProjectElementEventModel) {
					((PermissionChangedProjectElementEventModel) model).setProject(project.transformToReference());
				}
			} else {
				log.warn("The project for element {" + getUuid() + "} could not be found.");
			}
		}

	}

}
