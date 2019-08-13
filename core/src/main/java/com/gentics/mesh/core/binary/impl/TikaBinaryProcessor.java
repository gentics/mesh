package com.gentics.mesh.core.binary.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.binary.DocumentTikaParser;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.Lazy;
import io.reactivex.Maybe;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.RxHelper;

@Singleton
public class TikaBinaryProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TikaBinaryProcessor.class);

	private final Set<String> acceptedTypes = new HashSet<>();

	private final Set<String> acceptedDocumentTypes = new HashSet<>();

	private final Set<String> skipSet = new HashSet<>();

	private final Lazy<Vertx> vertx;

	private final MeshOptions options;

	/**
	 * Default limit for non-document binaries
	 */
	private static final int DEFAULT_NON_DOC_TIKA_PARSE_LIMIT = 0;

	@Inject
	public TikaBinaryProcessor(Lazy<Vertx> vertx, MeshOptions options) {
		this.vertx = vertx;
		this.options = options;

		// document
		acceptedDocumentTypes.add("text/plain");
		acceptedDocumentTypes.add("application/rtf");
		acceptedDocumentTypes.add("application/pdf");
		acceptedDocumentTypes.add("application/msword");
		acceptedDocumentTypes.add("application/vnd.");
		acceptedTypes.addAll(acceptedDocumentTypes);

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
		boolean accepted = acceptedTypes.stream().anyMatch(type -> {
			return contentType.startsWith(type);
		});
		if (log.isDebugEnabled()) {
			String mode = accepted ? "Accepting" : "Rejecting";
			log.debug(mode + " {" + contentType + "} for processor {" + getClass().getName() + "}");
		}
		return accepted;
	}

	@Override
	public Maybe<Consumer<BinaryGraphField>> process(FileUpload upload, String hash) {
		Maybe<Consumer<BinaryGraphField>> result = Maybe.create(sub -> {
			File uploadFile = new File(upload.uploadedFileName());
			if (log.isDebugEnabled()) {
				log.debug("Parsing file {" + uploadFile + "}");
			}

			int len = getParserLimit(upload.contentType());
			if (log.isDebugEnabled()) {
				log.debug("Using parser limit of {" + len + "}");
			}

			try (FileInputStream ins = new FileInputStream(uploadFile)) {
				TikaResult pr = parseFile(ins, len);

				Consumer<BinaryGraphField> consumer = field -> {
					pr.getMetadata().forEach((e, k) -> {
						field.setMetadata(e, k);
					});
					if (pr.getPlainText().isPresent()) {
						field.setPlainText(pr.getPlainText().get());
					}
					if (pr.getLoc().isPresent()) {
						field.setLocation(pr.getLoc());
					}
				};
				sub.onSuccess(consumer);
			} catch (Exception e) {
				log.warn("Tika processing of upload failed", e);
				sub.onError(e);
			}
		});

		return result.observeOn(RxHelper.blockingScheduler(vertx.get(), false)).onErrorComplete();

	}

	public TikaResult parseFile(InputStream ins, int len) throws TikaException, IOException {

		Location loc = new Location();
		Map<String, String> fields = new HashedMap<>();

		Metadata metadata = new Metadata();

		Optional<String> content = DocumentTikaParser.parse(ins, metadata, len);
		if (content.isPresent()) {
			log.debug("Got content {" + content.get() + "}");
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
			fields.put(name, value);
		}
		return new TikaResult(fields, content, loc);

	}

	public int getParserLimit(String contentType) {
		boolean isDocument = acceptedDocumentTypes.stream().anyMatch(type -> {
			return contentType.startsWith(type);
		});
		if (isDocument && options.getUploadOptions() != null) {
			return options.getUploadOptions().getParserLimit();
		} else {
			return DEFAULT_NON_DOC_TIKA_PARSE_LIMIT;
		}
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