package com.gentics.mesh.core.binary.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;

@Singleton
public class TikaBinaryProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TikaBinaryProcessor.class);

	private final Set<String> acceptedTypes = new HashSet<>();

	private final Set<String> skipSet = new HashSet<>();

	private final Parser parser = new AutoDetectParser();

	private final Database db;

	@Inject
	public TikaBinaryProcessor(Database db) {
		this.db = db;
		// Accepted types
		acceptedTypes.add("application/pdf");
		acceptedTypes.add("application/msword");
		acceptedTypes.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");

		// image
		acceptedTypes.add("image/jpeg");
		acceptedTypes.add("image/jpg");
		acceptedTypes.add("image/png");

		// audio
		acceptedTypes.add("audio/flac");
		acceptedTypes.add("audio/mp3");
		acceptedTypes.add("audio/ogg");
		acceptedTypes.add("audio/x-matroska");

		// video
		acceptedTypes.add("video/mp4");
		acceptedTypes.add("video/ogg");
		acceptedTypes.add("video/x-matroska");

		// Attribute keys to ignore
		skipSet.add("Content-Type");
		skipSet.add("X-Parsed-By");
		skipSet.add("access_permission_can_print_degraded");
		skipSet.add("access_permission_can_modify");
		skipSet.add("access_permission_extract_content");
		skipSet.add("access_permission_assemble_document");
		skipSet.add("access_permission_can_print");
		skipSet.add("access_permission_modify_annotations");
		skipSet.add("access_permission_extract_for_accessibility");
		skipSet.add("access_permission_fill_in_form");
		skipSet.add("File_Name");

	}

	@Override
	public boolean accepts(String contentType) {
		return acceptedTypes.contains(contentType);
	}

	@Override
	public Completable process(FileUpload upload, BinaryGraphField field) {

		File uploadFile = new File(upload.uploadedFileName());
		return db.asyncTx(() -> {
			try (FileInputStream inputstream = new FileInputStream(uploadFile)) {
				Metadata metadata = new Metadata();
				ParseContext context = new ParseContext();
				BodyContentHandler handler = new BodyContentHandler();

				// PDF files need to be parsed fully
				if (upload.contentType().toLowerCase().startsWith("application/pdf")) {
					handler = new BodyContentHandler(-1);
				}

				parser.parse(inputstream, handler, metadata, context);
				if (log.isDebugEnabled()) {
					log.debug("Parsed file {" + uploadFile + "} got content: {" + handler.toString() + "}");
				}

				String[] metadataNames = metadata.names();
				Location loc = new Location();
				for (String name : metadataNames) {
					String value = metadata.get(name);
					name = sanitizeName(name);
					if (skipSet.contains(name)) {
						log.debug("Skipping entry {" + name + "} because it is on the skip set.");
						continue;
					}
					if (value == null) {
						log.debug("Skipping entry {" + name + "} because value is null.");
						continue;
					}

					// Dedicated handling of GPS information
					try {
						if (name.equals("geo_lat")) {
							loc.setLat(Double.valueOf(value));
							continue;
						}
						if (name.equals("geo_long")) {
							loc.setLon(Double.valueOf(value));
							continue;
						}
						if (name.equals("GPS_Altitude")) {
							String v = value.replaceAll(" .*", "");
							loc.setAlt(Integer.parseInt(v));
							continue;
						}
					} catch (NumberFormatException e) {
						log.warn("Could not parse {" + name + "} key with value {" + value + "} - Ignoring field.", e);
					}

					log.debug("Adding property {" + name + "}={" + value + "}");
					field.setMetadata(name, value);
				}

				if (loc.isPresent()) {
					field.setLocation(loc);
				}
			} catch (Exception e) {
				log.warn("Tika processing of upload failed", e);
			}
		});
	}

	/**
	 * Replace characters from the key which can't be used in properties with underscores.
	 * 
	 * @param key
	 * @return
	 */
	public static String sanitizeName(String key) {
		key = key.replaceAll(":", "_");
		key = key.replaceAll("\\.", "_");
		key = key.replaceAll(",", "_");
		key = key.replaceAll(" ", "_");
		return key;
	}

}
