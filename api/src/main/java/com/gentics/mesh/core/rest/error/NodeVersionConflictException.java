package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception which will be thrown if a conflict was detected during a node update.
 */
public class NodeVersionConflictException extends AbstractRestException {

	public static final String TYPE = "node_version_conflict";

	private static final long serialVersionUID = -7624719224170510923L;

	private List<String> conflicts = new ArrayList<>();

	private String oldVersion;
	private String newVersion;

	public NodeVersionConflictException() {
	}

	/**
	 * Create a new node version conflict using the provided i18n message and i18n properties.
	 * 
	 * @param i18nMessage
	 * @param i18nProperties
	 */
	public NodeVersionConflictException(String i18nMessage, String... i18nProperties) {
		super(CONFLICT, i18nMessage, i18nProperties);
	}

	/**
	 * Add the given field key to the list of conflicting fields.
	 */
	public void addConflict(String fieldKey) {
		conflicts.add(fieldKey);
	}

	/**
	 * Return the list of fields which contain a conflict.
	 */
	public List<String> getConflicts() {
		return conflicts;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	/**
	 * Return the old node version which was involved in the conflict.
	 * 
	 * @return
	 */
	public String getOldVersion() {
		return oldVersion;
	}

	/**
	 * Set the old node version which was involved in the conflict.
	 * 
	 * @param oldVersion
	 */
	public void setOldVersion(String oldVersion) {
		this.oldVersion = oldVersion;
	}

	/**
	 * Return the new version which was involved in the conflict.
	 * 
	 * @return
	 */
	public String getNewVersion() {
		return newVersion;
	}

	/**
	 * Set the new version which was involved in the conflict.
	 * 
	 * @param newVersion
	 */
	public void setNewVersion(String newVersion) {
		this.newVersion = newVersion;
	}

}
