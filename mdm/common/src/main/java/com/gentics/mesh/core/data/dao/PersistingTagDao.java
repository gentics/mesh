package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Objects;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.db.Tx;

/**
 * A persisting extension to {@link TagDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingTagDao extends TagDao, PersistingDaoGlobal<HibTag> {

	@Override
	default HibTag loadObjectByUuid(HibBranch branch, InternalActionContext ac, String tagUuid, InternalPermission perm) {
		HibTag tag = branch.findTagByUuid(tagUuid);
		return checkPerms(tag, tagUuid, ac, perm, true);
	}

	@Override
	default HibTag findByUuid(HibProject project, String uuid) {
		return StreamSupport.stream(Tx.get().tagFamilyDao().findAll(project).spliterator(), false)
			.map(tagFamily -> findByUuid(tagFamily, uuid))
			.filter(Objects::nonNull)
			.findAny()
			.orElse(null);
	}

	@Override
	default HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm) {
		return loadObjectByUuid(project, ac, uuid, perm, true);
	}

	/**
	 * Find the tag with given UUID among the tag families of a given project.
	 * 
	 * @param project
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	default HibTag loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm,
			boolean errorIfNotFound) {
		return Tx.get().tagFamilyDao().findAllStream(project, ac, perm)
				.map(tagFamily -> loadObjectByUuid(tagFamily, ac, uuid, perm, false))
				.filter(Objects::nonNull)
				.map(tag -> checkPerms(tag, uuid, ac, perm, errorIfNotFound))
				.findAny()
				.orElseGet(() -> {
					if (errorIfNotFound) {
						throw error(NOT_FOUND, "object_not_found_for_uuid", uuid);
					} else {
						return null;
					}
				});
	}

}
