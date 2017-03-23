package com.gentics.mesh.core.rest.error;

public class PermissionException extends AbstractRestException {

	public static final String TYPE = "missing_perm";

	private static final long serialVersionUID = 6097093959066715614L;
	public static final String i18nKey = "graphql_error_missing_perm";

	private String elementId;
	private String elementType;

	public PermissionException(String elementId, String elementType) {
		super(null, i18nKey, elementId, elementType);
		this.elementId = elementId;
		this.elementType = elementType;
	}

	@Override
	public String getType() {
		return TYPE;
	}

	public String getElementType() {
		return elementType;
	}

	public String getElementId() {
		return elementId;
	}

}
