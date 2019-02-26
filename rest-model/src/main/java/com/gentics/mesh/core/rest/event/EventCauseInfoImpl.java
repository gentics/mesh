package com.gentics.mesh.core.rest.event;

import com.gentics.mesh.ElementType;

public class EventCauseInfoImpl implements EventCauseInfo {

	private ElementType type;
	private String uuid;
	private EventCauseAction action;

	public EventCauseInfoImpl() {
	}

	public EventCauseInfoImpl(ElementType elementType, String elementUuid, EventCauseAction action) {
		this.type = elementType;
		this.uuid = elementUuid;
		this.action = action;
	}

	@Override
	public ElementType getType() {
		return type;
	}

	public void setType(ElementType type) {
		this.type = type;
	}

	@Override
	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	@Override
	public EventCauseAction getAction() {
		return action;
	}

	public void setAction(EventCauseAction action) {
		this.action = action;
	}

}
