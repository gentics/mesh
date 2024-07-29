package com.gentics.mesh.hibernate.data.domain;

import java.util.function.Function;

import jakarta.persistence.Embedded;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Common part for the entities, that consider keeping the Mesh user creator/editor, and the corresponding timestamps.
 * 
 * @author plyhun
 *
 * @param <R>
 */
@MappedSuperclass
public abstract class AbstractHibUserTrackedElement<R extends RestModel> extends AbstractHibNamedElement<R> implements HibCreatorTracking, HibEditorTracking {

	@Embedded
	protected UserTracking userTracking = new UserTracking();
	
	@Override
	public HibUser getCreator() {
		return nullSafe(UserTracking::getCreator);
	}

	@Override
	public void setCreator(HibUser user) {
		userTracking.setCreator(user);
	}

	@Override
	public void setCreated(HibUser creator) {
		userTracking.setCreated(creator);
	}

	@Override
	public void setCreationTimestamp(long timestamp) {
		userTracking.setCreationTimestamp(timestamp);
	}

	@Override
	public void setCreationTimestamp() {
		userTracking.setCreationTimestamp(System.currentTimeMillis());
	}

	@Override
	public Long getCreationTimestamp() {
		return nullSafe(UserTracking::getCreationTimestamp);
	}

	@Override
	public HibUser getEditor() {
		return nullSafe(UserTracking::getEditor);
	}

	@Override
	public void setEditor(HibUser user) {
		userTracking.setEditor(user);
	}

	@Override
	public Long getLastEditedTimestamp() {
		return nullSafe(EditorTracking::getLastEditedTimestamp);
	}

	@Override
	public void setLastEditedTimestamp(long timestamp) {
		userTracking.setLastEditedTimestamp(timestamp);
	}

	@Override
	public void setLastEditedTimestamp() {
		userTracking.setLastEditedTimestamp(System.currentTimeMillis());
	}
	
	@Override
	public String getSubETag(InternalActionContext ac) {
		return String.valueOf(getLastEditedTimestamp());
	}

	private <T> T nullSafe(Function<UserTracking, T> provider) {
		if (userTracking == null) {
			// if you're wondering how user tracking can be null
			// https://hibernate.atlassian.net/browse/HHH-7610
			return null;
		}
		return provider.apply(userTracking);
	}
}
