package com.gentics.mesh.plugin.forms.export;

import java.io.File;

/**
 * Data wrapper for an asynchronous export.
 */
public class ExportData {

	/** The export format. */
	private final ExportFormat format;
	/** The path to the export file. */
	private final String exportFilePath;
	/** The download filename. */
	private final String downloadFilename;

	public ExportData(ExportFormat format, String exportFilePath, String downloadFilename) {
		this.format = format;
		this.exportFilePath = exportFilePath;
		this.downloadFilename = downloadFilename;
	}

	public ExportFormat getFormat() {
		return format;
	}

	public String getExportFilePath() {
		return exportFilePath;
	}

	public String getDownloadFilename() {
		return downloadFilename;
	}

	public File getExportFile() {
		return new File(exportFilePath);
	}
}
