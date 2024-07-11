package com.gentics.mesh.contentoperation;

/**
 * Special exception for the cases of no content table found for the (micro)schema version. It either means the version no longer existing, or the version creation being incomplete.
 * 
 * @author plyhun
 *
 */
public class ContentTableNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 5188748494083176417L;

	public ContentTableNotFoundException(String tableName) {
		super("Content table with name " + tableName + " was not found");
	}
}
