package com.gentics.mesh.core.data.util;

import java.util.Objects;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibInNode;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.syncleus.ferma.ElementFrame;

public final class HibClassConverter {

	private HibClassConverter() {
	}

	public static Tag toTag(HibTag tag) {
		return checkAndCast(tag, Tag.class);
	}

	public static TagFamily toTagFamily(HibTagFamily tagFamily) {
		return checkAndCast(tagFamily, TagFamily.class);
	}

	public static User toUser(HibUser user) {
		return checkAndCast(user, User.class);
	}

	public static BranchVersionEdge toBranchVersionEdge(HibBranchVersionAssignment assignment) {
		return checkAndCast(assignment, BranchVersionEdge.class);
	}

	public static GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> toVersion(HibFieldSchemaVersionElement element) {
		return checkAndCast(element, GraphFieldSchemaContainerVersion.class);
	}

	public static Group toGroup(HibGroup group) {
		return checkAndCast(group, Group.class);
	}

	public static Branch toBranch(HibBranch branch) {
		return checkAndCast(branch, Branch.class);
	}

	public static Schema toSchema(HibSchema schema) {
		return checkAndCast(schema, Schema.class);
	}

	public static SchemaVersion toSchemaVersion(HibSchemaVersion version) {
		return checkAndCast(version, SchemaVersion.class);
	}

	public static Microschema toMicroschema(HibMicroschema schema) {
		return checkAndCast(schema, Microschema.class);
	}

	public static MicroschemaVersion toMicroschemaVersion(HibMicroschemaVersion version) {
		return checkAndCast(version, MicroschemaVersion.class);
	}

	public static Project toProject(HibProject project) {
		return checkAndCast(project, Project.class);
	}

	public static Role toRole(HibRole role) {
		return checkAndCast(role, Role.class);
	}

	public static Binary toBinary(HibBinary binary) {
		return checkAndCast(binary, Binary.class);
	}

	/**
	 * @param node
	 * @return
	 * @deprecated Use {@link #toNode(HibNode)} instead.
	 */
	@Deprecated
	public static Node toNode(HibInNode node) {
		return checkAndCast(node, Node.class);
	}

	public static Node toNode(HibNode node) {
		return checkAndCast(node, Node.class);
	}

	public static Job toJob(HibJob job) {
		return checkAndCast(job, Job.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> T checkAndCast(HibElement element, Class<? extends ElementFrame> clazz) {
		Objects.requireNonNull(element, "The provided element was null and thus can't be converted to " + clazz.getName());
		if (clazz.isInstance(element)) {
			return (T) clazz.cast(element);
		} else {
			throw new RuntimeException("The received element was not an OrientDB element. Got: " + element.getClass().getName());
		}
	}

}
