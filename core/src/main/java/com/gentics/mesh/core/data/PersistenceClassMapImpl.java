package com.gentics.mesh.core.data;

import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.impl.BranchImpl;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.job.JobRoot;
import com.gentics.mesh.core.data.job.impl.JobRootImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.NodeRoot;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.root.impl.GroupRootImpl;
import com.gentics.mesh.core.data.root.impl.MeshRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.RoleRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.data.root.impl.UserRootImpl;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;

@Singleton
public class PersistenceClassMapImpl extends HashMap<Class<?>, Class<?>> implements PersistenceClassMap {

	private static final long serialVersionUID = -5867896951957817737L;

	@Inject
	public PersistenceClassMapImpl() {
		put(MeshRoot.class, MeshRootImpl.class);

		put(Node.class, NodeImpl.class);
		put(NodeGraphFieldContainer.class, NodeGraphFieldContainerImpl.class);
		put(User.class, UserImpl.class);
		put(Role.class, RoleImpl.class);
		put(Group.class, GroupImpl.class);

		put(TagFamily.class, TagFamilyImpl.class);
		put(Tag.class, TagImpl.class);
		put(Branch.class, BranchImpl.class);
		put(Project.class, ProjectImpl.class);
		put(MicroschemaContainer.class, MicroschemaContainerImpl.class);
		put(SchemaContainer.class, SchemaContainerImpl.class);

		put(MicroschemaContainerVersion.class, MicroschemaContainerVersionImpl.class);
		put(SchemaContainerVersion.class, SchemaContainerVersionImpl.class);

		put(UserRoot.class, UserRootImpl.class);
		put(GroupRoot.class, GroupRootImpl.class);
		put(RoleRoot.class, RoleRootImpl.class);
		put(JobRoot.class, JobRootImpl.class);

		put(NodeRoot.class, NodeRootImpl.class);
		put(ProjectRoot.class, ProjectRootImpl.class);
		put(TagFamilyRoot.class, TagFamilyRootImpl.class);
	}

}
