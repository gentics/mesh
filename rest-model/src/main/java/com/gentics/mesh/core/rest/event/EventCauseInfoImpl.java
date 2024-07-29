package com.gentics.mesh.core.rest.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.ElementType;

/**
 * @see EventCauseInfo
 */
public class EventCauseInfoImpl implements EventCauseInfo {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Type of the element that is referenced by the cause.")
	private ElementType type;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Uuid of the element that is referenced by the cause.")
	private String uuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Action of the cause. A event can for example be cause by a delete action.")
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
