package com.gentics.mesh.core.rest;

import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.rest.branch.BranchReference;
import com.gentics.mesh.core.rest.event.branch.BranchMeshEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchMicroschemaAssignModel;
import com.gentics.mesh.core.rest.event.branch.BranchSchemaAssignEventModel;
import com.gentics.mesh.core.rest.event.branch.BranchTaggedEventModel;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.job.ProjectVersionPurgeEventModel;
import com.gentics.mesh.core.rest.event.migration.BranchMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.MicroschemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.event.node.NodeMovedEventModel;
import com.gentics.mesh.core.rest.event.node.NodeTaggedEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.tag.TagMeshEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Utility class which can be used to generate REST model examples.
 */
public final class Examples {

	private Examples() {
	}

	public static ProjectVersionPurgeEventModel versionPurgeEvent() {
		ProjectVersionPurgeEventModel model = new ProjectVersionPurgeEventModel();
		model.setUuid(uuid1());
		model.setName("demo");
		model.setStatus(JobStatus.RUNNING);
		return model;
	}

	public static SchemaMigrationMeshEventModel schemaMigrationEvent() {
		SchemaMigrationMeshEventModel model = new SchemaMigrationMeshEventModel();
		model.setUuid(uuid1());
		model.setBranch(branchRef());
		model.setProject(projectRef());
		model.setFromVersion(schemaRef("1"));
		model.setToVersion(schemaRef("2"));
		model.setStatus(JobStatus.RUNNING);
		return model;
	}

	public static MicroschemaMigrationMeshEventModel microschemaMigrationEvent() {
		MicroschemaMigrationMeshEventModel model = new MicroschemaMigrationMeshEventModel();
		model.setUuid(uuid1());
		model.setBranch(branchRef());
		model.setProject(projectRef());
		model.setFromVersion(microschemaRef("1"));
		model.setToVersion(microschemaRef("2"));
		model.setStatus(JobStatus.RUNNING);
		return model;
	}

	public static BranchMigrationMeshEventModel branchMigrationEvent() {
		BranchMigrationMeshEventModel model = new BranchMigrationMeshEventModel();
		model.setUuid(uuid1());
		model.setBranch(branchRef());
		model.setProject(projectRef());
		model.setStatus(JobStatus.RUNNING);
		return model;
	}

	public static BranchSchemaAssignEventModel schemaBranchAssignEvent() {
		BranchSchemaAssignEventModel model = new BranchSchemaAssignEventModel();
		model.setBranch(branchRef());
		model.setProject(projectRef());
		model.setSchema(schemaRef("1"));
		model.setStatus(JobStatus.COMPLETED);
		return model;
	}

	public static BranchMicroschemaAssignModel microschemaBranchAssignEvent() {
		BranchMicroschemaAssignModel model = new BranchMicroschemaAssignModel();
		model.setBranch(branchRef());
		model.setProject(projectRef());
		model.setSchema(microschemaRef("1"));
		model.setStatus(JobStatus.COMPLETED);
		return model;
	}

	public static MeshElementEventModelImpl userEvent() {
		return elementEvent("joedoe");
	}

	public static MeshElementEventModelImpl groupEvent() {
		return elementEvent("guests");
	}

	public static GroupUserAssignModel groupUserAssignEvent() {
		GroupUserAssignModel model = new GroupUserAssignModel();
		model.setUser(userRef());
		model.setGroup(groupRef());
		return model;
	}

	public static GroupRoleAssignModel groupRoleAssignEvent() {
		GroupRoleAssignModel model = new GroupRoleAssignModel();
		model.setRole(roleRef());
		model.setGroup(groupRef());
		return model;
	}

	public static MeshElementEventModelImpl roleEvent() {
		return elementEvent("translator");
	}

	public static PermissionChangedEventModel rolePermissionChangedEvent() {
		PermissionChangedEventModelImpl model = new PermissionChangedEventModelImpl();
		model.setName("editors");
		model.setUuid(uuid1());
		model.setRole(roleRef());
		model.setType(ElementType.GROUP);
		return model;
	}

	public static MeshElementEventModelImpl projectEvent() {
		return elementEvent("demo");
	}

	public static TagMeshEventModel tagEvent() {
		TagMeshEventModel model = new TagMeshEventModel();
		model.setName("red");
		model.setUuid(uuid1());
		model.setTagFamily(tagFamilyRef());
		model.setProject(projectRef());
		return model;
	}

	public static TagFamilyMeshEventModel tagFamilyEvent() {
		TagFamilyMeshEventModel model = new TagFamilyMeshEventModel();
		model.setProject(projectRef());
		model.setUuid(uuid1());
		model.setName("colors");
		return model;
	}

	public static NodeMeshEventModel nodeEvent() {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setBranchUuid(uuid2());
		model.setUuid(uuid1());
		model.setProject(projectRef());
		return model;
	}

	public static NodeMeshEventModel nodeContentEvent() {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setBranchUuid(uuid2());
		model.setUuid(uuid1());
		model.setLanguageTag("en");
		model.setType(PUBLISHED);
		model.setProject(projectRef());
		return model;
	}

	public static MeshElementEventModelImpl schemaEvent() {
		return elementEvent("content");
	}

	public static MeshElementEventModelImpl microschemaEvent() {
		return elementEvent("vcard");
	}

	public static BranchMeshEventModel branchEvent() {
		BranchMeshEventModel model = new BranchMeshEventModel();
		model.setName("demoV1");
		model.setUuid(uuid1());
		model.setProject(projectRef());
		return model;
	}

	public static BranchTaggedEventModel branchTaggingEvent() {
		BranchTaggedEventModel model = new BranchTaggedEventModel();
		model.setTag(tagRef());
		model.setProject(projectRef());
		model.setBranch(branchRef());
		return model;
	}

	public static NodeTaggedEventModel nodeTaggedEvent() {
		NodeTaggedEventModel model = new NodeTaggedEventModel();
		model.setBranch(branchRef());
		model.setTag(tagRef());
		return model;
	}

	public static NodeMovedEventModel nodeMovedEvent() {
		NodeMovedEventModel model = new NodeMovedEventModel();
		model.setBranchUuid(uuid2());
		model.setProject(projectRef());
		model.setSchema(schemaRef("1"));
		model.setTarget(nodeRef());
		return model;
	}

	public static MeshElementEventModelImpl elementEvent(String name) {
		MeshElementEventModelImpl model = new MeshElementEventModelImpl();
		model.setUuid(uuid1());
		model.setName(name);
		return model;
	}

	public static NodeReference nodeRef() {
		NodeReference ref = new NodeReference();
		ref.setUuid(uuid2());
		ref.setProjectName("demo");
		ref.setSchema(schemaRef("1"));
		return ref;
	}

	public static SchemaReference schemaRef(String version) {
		SchemaReference ref = new SchemaReferenceImpl();
		ref.setName("folder");
		ref.setUuid(uuid2());
		ref.setVersion(version);
		ref.setVersionUuid(uuid4());
		return ref;
	}

	public static MicroschemaReference microschemaRef(String version) {
		MicroschemaReference ref = new MicroschemaReferenceImpl();
		ref.setName("vcard");
		ref.setUuid(uuid2());
		ref.setVersion(version);
		ref.setVersionUuid(uuid4());
		return ref;
	}

	public static UserReference userRef() {
		UserReference ref = new UserReference();
		ref.setUuid(uuid4());
		ref.setFirstName("Joe");
		ref.setLastName("Doe");
		return ref;
	}

	public static GroupReference groupRef() {
		GroupReference ref = new GroupReference();
		ref.setUuid(uuid4());
		ref.setName("guests");
		return ref;
	}

	public static RoleReference roleRef() {
		RoleReference ref = new RoleReference();
		ref.setUuid(uuid4());
		ref.setName("translator");
		return ref;
	}

	public static TagReference tagRef() {
		String uuid = "e5861ba26b914b21861ba26b91ab211a";
		TagReference ref = new TagReference();
		ref.setName("red");
		ref.setTagFamily("colors");
		ref.setUuid(uuid);
		return ref;
	}

	public static TagFamilyReference tagFamilyRef() {
		TagFamilyReference ref = new TagFamilyReference();
		ref.setUuid(uuid2());
		ref.setName("colors");
		return ref;
	}

	public static BranchReference branchRef() {
		String uuid = "b12272150db4490ea272150db4190e72";
		BranchReference ref = new BranchReference();
		ref.setName("demoV1");
		ref.setUuid(uuid);
		return ref;
	}

	public static ProjectReference projectRef() {
		String uuid = "ec2f5106764c4d94af5106764ccd948a";
		return new ProjectReference().setName("demo").setUuid(uuid);
	}

	public static String uuid1() {
		return "2619185de0db4a5399185de0dbda53da";
	}

	public static String uuid2() {
		return "cd6b4f2851814773ab4f28518137735f";
	}

	public static String uuid3() {
		return "079ae5d5467447b99ae5d5467447b934";
	}

	public static String uuid4() {
		return "d84a6f054a3f4ed68a6f054a3f1ed635";
	}

}
