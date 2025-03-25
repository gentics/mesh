package com.gentics.mesh.hibernate.data.domain;

import java.time.Instant;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.user.HibUser;

/**
 * Embeddable part of editor user/timestamp
 * 
 * @see AbstractHibUserTrackedElement
 * @author plyhun
 *
 */
@Embeddable
public class EditorTracking {

	@ManyToOne(targetEntity = HibUserImpl.class, fetch = FetchType.LAZY)
	private HibUser editor;

	private Instant edited;

	public HibUser getEditor() {
		return editor;
	}

	public void setEditor(HibUser user) {
		this.editor = user;
	}

	public Long getLastEditedTimestamp() {
		return edited != null ? edited.toEpochMilli() : null;
	}

	public void setLastEditedTimestamp(Long timestamp) {
		edited = timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
	}

	protected void setEdited(Instant edited) {
		this.edited = edited;
	}
}
