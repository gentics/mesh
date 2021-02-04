package com.gentics.mesh.core.data.util;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibContent;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
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
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.syncleus.ferma.ElementFrame;

/**
 * This converted is created for the MDM migration refactoring purposes. It should be gone when the refactoring is finished.
 * Should not be used in the new code.
 * 
 * @author plyhun
 *
 */
@Deprecated
public final class HibClassConverter {

	private HibClassConverter() {
	}

	public static MeshElement toGraph(HibBaseElement element) {
		return checkAndCast(element, MeshElement.class);
	}

	public static Tag toGraph(HibTag tag) {
		return checkAndCast(tag, Tag.class);
	}

	public static TagFamily toGraph(HibTagFamily tagFamily) {
		return checkAndCast(tagFamily, TagFamily.class);
	}

	public static User toGraph(MeshAuthUser user) {
		if (user == null) {
			return null;
		}
		return toGraph(user.getDelegate());
	}

	public static User toGraph(HibUser user) {
		if (user instanceof MeshAuthUser) {
			return toGraph((MeshAuthUser) user);
		}
		return checkAndCast(user, User.class);
	}

	public static BranchVersionEdge toGraph(HibBranchVersionAssignment assignment) {
		return checkAndCast(assignment, BranchVersionEdge.class);
	}

	public static <SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>, R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion> GraphFieldSchemaContainer<R, ?, SC, SCV> toGraphContainer(
		HibFieldSchemaElement<R, RM, SC, SCV> element) {
		return checkAndCast(element, GraphFieldSchemaContainer.class);
	}

	public static <RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>, R extends FieldSchemaContainer> GraphFieldSchemaContainerVersion<R, RM, ?, SCV, ?> toGraphVersion(
		HibFieldSchemaVersionElement<R, RM, SC, SCV> element) {
		return checkAndCast(element, GraphFieldSchemaContainerVersion.class);
	}

	public static Group toGraph(HibGroup group) {
		return checkAndCast(group, Group.class);
	}

	public static Branch toGraph(HibBranch branch) {
		return checkAndCast(branch, Branch.class);
	}

	public static Schema toGraph(HibSchema schema) {
		return checkAndCast(schema, Schema.class);
	}

	public static SchemaVersion toGraph(HibSchemaVersion version) {
		return checkAndCast(version, SchemaVersion.class);
	}

	public static Microschema toGraph(HibMicroschema schema) {
		return checkAndCast(schema, Microschema.class);
	}

	public static MicroschemaVersion toGraph(HibMicroschemaVersion version) {
		return checkAndCast(version, MicroschemaVersion.class);
	}

	public static Project toGraph(HibProject project) {
		return checkAndCast(project, Project.class);
	}

	public static Role toGraph(HibRole role) {
		return checkAndCast(role, Role.class);
	}

	public static Binary toGraph(HibBinary binary) {
		return checkAndCast(binary, Binary.class);
	}

	public static Node toGraph(HibNode node) {
		return checkAndCast(node, Node.class);
	}

	public static NodeGraphFieldContainer toGraph(HibContent content) {
		return checkAndCast(content, NodeGraphFieldContainer.class);
	}

	public static Job toGraph(HibJob job) {
		return checkAndCast(job, Job.class);
	}

	public static SchemaChange<?> toGraph(HibSchemaChange<?> change) {
		return checkAndCast(change, SchemaChange.class);
	}

	public static GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> toGraph(HibFieldSchemaVersionElement<?, ?, ?, ?> version) {
		return checkAndCast(version, GraphFieldSchemaContainerVersion.class);
	}
	
	public static NodeGraphFieldContainer toGraph(HibNodeFieldContainer container) {
		return checkAndCast(container, NodeGraphFieldContainer.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> T checkAndCast(HibElement element, Class<? extends ElementFrame> clazz) {
		if (element == null) {
			return null;
		}
		if (clazz.isInstance(element)) {
			return (T) clazz.cast(element);
		} else {
			throw new RuntimeException("The received element was not an OrientDB element. Got: " + element.getClass().getName());
		}
	}

}
