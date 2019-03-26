package com.gentics.mesh.core.rest.error;

import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Exception which will be thrown if a conflict was detected during a node update.
 */
public class NodeVersionConflictException extends AbstractRestException {

	public static final String TYPE = "node_version_conflict";

	private static final long serialVersionUID = -7624719224170510923L;

	private static final String CONFLICT_KEY = "conflicts";
	private static final String NEW_VERSION_KEY = "newVersion";
	private static final String OLD_VERSION_KEY = "oldVersion";

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
		getConflicts().add(fieldKey);
	}

	@Override
	public String toString() {
		return super.toString() + " conflicts {" + String.join(",", getConflicts()) + "} old {" + getOldVersion() + "}" + " new {" + getNewVersion()
			+ "}";
	}

	/**
	 * Return the list of fields which contain a conflict.
	 */
	@JsonIgnore
	public List<String> getConflicts() {
		List<String> list = (List<String>) getProperties().get(CONFLICT_KEY);
		if (list == null) {
			list = new ArrayList<String>();
			getProperties().put(CONFLICT_KEY, list);
		}
		return list;
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
	@JsonIgnore
	public String getOldVersion() {
		return getProperty(OLD_VERSION_KEY);
	}

	/**
	 * Set the old node version which was involved in the conflict.
	 * 
	 * @param oldVersion
	 */
	public void setOldVersion(String oldVersion) {
		setProperty(OLD_VERSION_KEY, oldVersion);
	}

	/**
	 * Return the new version which was involved in the conflict.
	 * 
	 * @return
	 */
	@JsonIgnore
	public String getNewVersion() {
		return getProperty(NEW_VERSION_KEY);
	}

	/**
	 * Set the new version which was involved in the conflict.
	 * 
	 * @param newVersion
	 */
	public void setNewVersion(String newVersion) {
		setProperty(NEW_VERSION_KEY, newVersion);
	}

}
