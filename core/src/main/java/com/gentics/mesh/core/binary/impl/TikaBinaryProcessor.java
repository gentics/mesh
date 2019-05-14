package com.gentics.mesh.core.binary.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.io.IOUtils;
import org.apache.tika.metadata.Metadata;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.binary.DocumentTikaParser;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;

import io.reactivex.Maybe;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.RxHelper;

@Singleton
public class TikaBinaryProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TikaBinaryProcessor.class);

	private final Set<String> acceptedTypes = new HashSet<>();

	private final Set<String> skipSet = new HashSet<>();

	private static final int DEFAULT_TIKA_CHAR_LIMIT = 100000;

	private final MeshOptions options;

	@Inject
	public TikaBinaryProcessor(MeshOptions options) {
		this.options = options;

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
	public Maybe<Consumer<BinaryGraphField>> process(FileUpload upload, String hash) {

		File uploadFile = new File(upload.uploadedFileName());
		Maybe<Consumer<BinaryGraphField>> result = Maybe.create(sub -> {
			Location loc = new Location();
			Map<String, String> fields = new HashedMap<>();
			try (FileInputStream inputstream = new FileInputStream(uploadFile)) {
				Metadata metadata = new Metadata();

				// PDF files need to be parsed fully
				int len = DEFAULT_TIKA_CHAR_LIMIT;
				if (needsFullParsing(upload)) {
					len = -1;
				}

				Optional<String> content = DocumentTikaParser.parse(inputstream, metadata, len);
				if (log.isDebugEnabled()) {
					log.debug("Parsed file {" + uploadFile + "}");
				}
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

				// Cache fields
				ElasticSearchOptions searchOptions = options.getSearchOptions();
				if (searchOptions != null && searchOptions.isProcessBinary() && searchOptions.getMetadataCacheDirectory() != null) {
					if (content.isPresent()) {
						File cacheFile = new File(searchOptions.getMetadataCacheDirectory());
						if (!cacheFile.exists()) {
							try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
								IOUtils.write(content.get(), fos);
							}
						} else {
							log.debug("Cache file {" + cacheFile + "} found. No need to write file.");
						}
					}
				} else {
					log.debug("Binary processing or metadata cache directory not set. Not caching metadata.");
				}

				Consumer<BinaryGraphField> consumer = field -> {
					fields.forEach((e, k) -> {
						field.setMetadata(e, k);
					});
					if (loc.isPresent()) {
						field.setLocation(loc);
					}
				};
				sub.onSuccess(consumer);
			} catch (Exception e) {
				log.warn("Tika processing of upload failed", e);
				sub.onError(e);
			}
		});

		return result.observeOn(RxHelper.blockingScheduler(Mesh.vertx(), false)).onErrorComplete();

	}

	private boolean needsFullParsing(FileUpload upload) {
		return upload.contentType().toLowerCase().startsWith("application/pdf");
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
