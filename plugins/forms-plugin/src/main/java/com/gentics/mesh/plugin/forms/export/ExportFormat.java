package com.gentics.mesh.plugin.forms.export;

/**
 * Supported formats for exporting form generator data with information about
 * file extension and MIME type.
 */
public enum ExportFormat {
	CSV("csv", "text/csv; charset=utf-8");

	public final String fileExtension;
	public final String mimeType;

	ExportFormat(String fileExtension, String mimeType) {
		this.fileExtension = fileExtension;
		this.mimeType = mimeType;
	}
}
