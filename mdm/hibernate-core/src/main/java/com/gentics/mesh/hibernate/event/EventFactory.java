package com.gentics.mesh.hibernate.event;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

import javax.inject.Inject;

import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.HibProjectElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.Assignment;

import org.slf4j.Logger;

/**
 * Event factory component. Serves the creation of various {@link MeshElementEventModel}s.
 * 
 * @author plyhun
 *
 */
public class EventFactory {
	private static final Logger log = getLogger(EventFactory.class);

	private final HibernateMeshOptions options;

	@Inject
	public EventFactory(MeshOptions options) {
		this.options = (HibernateMeshOptions) options;
	}

	public MeshElementEventModel onDeleted(HibCoreElement<? extends RestModel> entity) {
		return add(entity, entity.getTypeInfo().getOnDeleted());
	}

	public MeshElementEventModel onCreated(HibCoreElement<? extends RestModel> entity) {
		return add(entity, entity.getTypeInfo().getOnCreated());
	}

	public MeshElementEventModel onUpdated(HibCoreElement<? extends RestModel> entity) {
		return add(entity, entity.getTypeInfo().getOnUpdated());
	}

	private MeshElementEventModel add(HibCoreElement<? extends RestModel> entity, MeshEvent event) {
		MeshElementEventModel model = new MeshElementEventModelImpl();
		model.setEvent(event);
		fillEventInfo(entity, model);
		return model;
	}

	private void fillEventInfo(HibCoreElement<? extends RestModel> entity, MeshElementEventModel model) {
		if (entity instanceof HibNamedElement) {
			model.setName(((HibNamedElement) entity).getName());
		}
		model.setOrigin(options.getNodeName());
		model.setUuid(entity.getUuid());
	}

	public MeshEventModel onRoleAssignmentEvent(HibGroup group, HibRole role, Assignment assignment) {
		GroupRoleAssignModel model = new GroupRoleAssignModel();
		model.setGroup(group.transformToReference());
		model.setRole(role.transformToReference());
		switch (assignment) {
			case ASSIGNED:
				model.setEvent(GROUP_ROLE_ASSIGNED);
				break;
			case UNASSIGNED:
				model.setEvent(GROUP_ROLE_UNASSIGNED);
				break;
		}
		return model;
	}

	public MeshEventModel onUserAssignmentEvent(HibUser user, HibGroup group, Assignment assignment) {
		GroupUserAssignModel model = new GroupUserAssignModel();
		model.setGroup(group.transformToReference());
		model.setUser(user.transformToReference());
		switch (assignment) {
			case ASSIGNED:
				model.setEvent(GROUP_USER_ASSIGNED);
				break;
			case UNASSIGNED:
				model.setEvent(GROUP_USER_UNASSIGNED);
				break;
		}
		return model;
	}
	
	public PermissionChangedEventModelImpl onPermissionChanged(HibCoreElement<? extends RestModel> entity, HibRole role) {
		PermissionChangedEventModelImpl model = new PermissionChangedEventModelImpl();
		fillPermissionChanged(entity, model, role);
		return model;
	}

	public void fillPermissionChanged(HibCoreElement<? extends RestModel> entity, PermissionChangedEventModelImpl model, HibRole role) {
		model.setEvent(ROLE_PERMISSIONS_CHANGED);
		model.setRole(role.transformToReference());
		model.setType(entity.getTypeInfo().getType());
		fillEventInfo(entity, model);
		if (entity instanceof HibProjectElement) {
			HibProject project = ((HibProjectElement) entity).getProject();
			if (project != null) {
				if (model instanceof PermissionChangedProjectElementEventModel) {
					((PermissionChangedProjectElementEventModel) model).setProject(project.transformToReference());
				}
			} else {
				log.warn("The project for element {" + entity.getUuid() + "} could not be found.");
			}
		}
	}
}
