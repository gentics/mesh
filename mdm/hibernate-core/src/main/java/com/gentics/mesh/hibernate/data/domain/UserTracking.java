package com.gentics.mesh.hibernate.data.domain;

import java.time.Instant;

import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import com.gentics.mesh.core.data.user.HibUser;

/**
 * Embeddable part of creator user/timestamp
 * 
 * @see AbstractHibUserTrackedElement
 * @author plyhun
 *
 */
@Embeddable
public class UserTracking extends EditorTracking {

	@ManyToOne(targetEntity = HibUserImpl.class, fetch = FetchType.LAZY)
	private HibUser creator;

	private Instant created;

	public HibUser getCreator() {
		return creator;
	}

	public void setCreator(HibUser user) {
		this.creator = user;
	}

	public Long getCreationTimestamp() {
		return created.toEpochMilli();
	}

	public void setCreationTimestamp(Long timestamp) {
		this.created = timestamp != null ? Instant.ofEpochMilli(timestamp) : null;
	}

	public void setCreated(HibUser creator) {
		Instant now = Instant.now();
		setCreator(creator);
		setEditor(creator);
		setCreated(now);
		setEdited(now);
	}

	protected void setCreated(Instant created) {
		this.created = created;
	}
}
