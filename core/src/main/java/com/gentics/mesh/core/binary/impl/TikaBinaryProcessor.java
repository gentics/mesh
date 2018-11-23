package com.gentics.mesh.core.binary.impl;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.binary.Location;

import io.reactivex.Single;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;

@Singleton
public class TikaBinaryProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TikaBinaryProcessor.class);

	private final Set<String> acceptedTypes = new HashSet<>();

	private final Set<String> skipSet = new HashSet<>();

	private final Parser parser = new AutoDetectParser();

	@Inject
	public TikaBinaryProcessor() {
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
	public Single<Consumer<BinaryGraphField>> process(FileUpload upload) {

		Map<String, String> metadataMap = new HashedMap<>();
		Location loc = new Location();
		File uploadFile = new File(upload.uploadedFileName());
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
				metadata.add(name, value);
			}

		} catch (Exception e) {
			log.warn("Tika processing of upload failed", e);
		}

		return Single.just((field) -> {
			for (Entry<String, String> entry : metadataMap.entrySet()) {
				field.setMetadata(entry.getKey(), entry.getValue());
			}
			if (loc.isPresent()) {
				field.setLocation(loc);
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
