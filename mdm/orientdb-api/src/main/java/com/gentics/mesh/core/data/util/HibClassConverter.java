package com.gentics.mesh.core.data.util;

import java.util.Objects;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.graphdb.model.MeshElement;

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

	public static Group toGroup(HibGroup group) {
		return checkAndCast(group, Group.class);
	}

	public static Branch toBranch(HibBranch branch) {
		return checkAndCast(branch, Branch.class);
	}

	public static Project toProject(HibProject project) {
		return checkAndCast(project, Project.class);
	}

	@SuppressWarnings("unchecked")
	public static <T> T checkAndCast(HibElement element, Class<? extends MeshElement> clazz) {
		Objects.requireNonNull(element, "The provided element was null and thus can't be converted to " + clazz.getName());
		if (clazz.isInstance(element)) {
			return (T) clazz.cast(element);
		} else {
			throw new RuntimeException("The received element was not an OrientDB element. Got: " + element.getClass().getName());
		}
	}

}
