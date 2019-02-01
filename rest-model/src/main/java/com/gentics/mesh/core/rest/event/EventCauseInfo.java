package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.core.rest.common.RestModel;

public class EventCauseInfo implements RestModel {

	private String type;
	private String uuid;
	private String action;

	public EventCauseInfo(String elementType, String elementUuid, String action) {
		this.type = elementType;
		this.uuid = elementUuid;
		this.action = action;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

}
