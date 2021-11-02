package com.gentics.mesh.core.binary.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.binary.BinaryDataProcessorContext;
import com.gentics.mesh.core.binary.DocumentTikaParser;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.binary.Location;
import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.etc.config.MeshOptions;

import dagger.Lazy;
import io.reactivex.Maybe;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.reactivex.core.Vertx;

/**
 * This class can be used to parse binary data from {@link BinaryGraphField} fields. Once parsed the processor will populate the field with additional meta data
 * from the parsing result.
 */
@Singleton
public class TikaBinaryProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TikaBinaryProcessor.class);

	private final Set<String> acceptedTypes = new HashSet<>();

	private final Set<String> acceptedDocumentTypes = new HashSet<>();

	private final Set<String> skipSet = new HashSet<>();

	private final Lazy<Vertx> vertx;

	private final MeshOptions options;

	private final Database db;

	/**
	 * Default limit for non-document binaries
	 */
	private static final int DEFAULT_NON_DOC_TIKA_PARSE_LIMIT = 0;

	@Inject
	public TikaBinaryProcessor(Lazy<Vertx> vertx, MeshOptions options, Database db) {
		this.vertx = vertx;
		this.options = options;
		this.db = db;

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
	public Maybe<Consumer<HibBinaryField>> process(BinaryDataProcessorContext ctx) {
		FileUpload upload = ctx.getUpload();
		return getExtractOptions(ctx.getActionContext(), ctx.getNodeUuid(), ctx.getFieldName())
			.flatMap(
				extractOptions -> process(extractOptions, upload),
				Maybe::error,
				() -> process(null, upload));
	}

	/**
	 * Determines if the binary data will be processed for the node.
	 * 
	 * @param ac
	 * @param nodeUuid
	 * @param fieldName
	 * @return
	 */
	private Maybe<BinaryExtractOptions> getExtractOptions(InternalActionContext ac, String nodeUuid, String fieldName) {
		return db.maybeTx(tx -> {
			NodeDao nodeDao = tx.nodeDao();
			HibProject project = tx.getProject(ac);
			HibBranch branch = tx.getBranch(ac);
			HibNode node = nodeDao.loadObjectByUuid(project, ac, nodeUuid, InternalPermission.UPDATE_PERM);

			// Load the current latest draft
			HibNodeFieldContainer latestDraftVersion = node.getFieldContainers(branch, ContainerType.DRAFT).next();

			FieldSchema fieldSchema = latestDraftVersion.getSchemaContainerVersion()
				.getSchema()
				.getField(fieldName);

			if (fieldSchema == null) {
				throw error(BAD_REQUEST, "error_schema_definition_not_found", fieldName);
			}

			if (fieldSchema instanceof BinaryFieldSchema) {
				return ((BinaryFieldSchema) fieldSchema).getBinaryExtractOptions();
			} else {
				throw error(BAD_REQUEST, "error_found_field_is_not_binary", fieldName);
			}

		});
	}

	private Maybe<Consumer<HibBinaryField>> process(BinaryExtractOptions extractOptions, FileUpload upload) {
		// Shortcut if no field specific options are specified and parsing is globally disabled
		if (!options.getUploadOptions().isParser() && extractOptions == null) {
			log.debug("Not parsing " + upload.fileName()
				+ " because it is globally disabled and no extract options are defined in the binary field schema.");
			return Maybe.empty();
		}

		// Shortcut if parsing is explicitly disabled for this binary field
		if (extractOptions != null && !extractOptions.getContent() && !extractOptions.getMetadata()) {
			log.debug("Not parsing " + upload.fileName() + " because it is explicitly disabled in the binary field schema.");
			return Maybe.empty();
		}

		return vertx.get().rxExecuteBlocking(promise -> {
			File uploadFile = new File(upload.uploadedFileName());
			if (log.isDebugEnabled()) {
				log.debug("Parsing file {" + uploadFile + "}");
			}

			int len = getParserLimit(extractOptions, upload.contentType());
			if (log.isDebugEnabled()) {
				log.debug("Using parser limit of {" + len + "}");
			}

			try (FileInputStream ins = new FileInputStream(uploadFile)) {
				boolean parseMetadata = extractOptions == null || extractOptions.getMetadata();
				TikaResult pr = parseFile(ins, len, parseMetadata);

				Consumer<HibBinaryField> consumer = field -> {
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
				promise.complete(consumer);
			} catch (Exception e) {
				log.warn("Tika processing of upload failed", e);
				promise.fail(e);
			}
		}, true);
	}

	/**
	 * Parse the given file via the provided input stream.
	 * 
	 * @param ins
	 * @param len
	 * @return
	 * @throws TikaException
	 * @throws IOException
	 */
	public TikaResult parseFile(InputStream ins, int len) throws TikaException, IOException {
		return parseFile(ins, len, true);
	}

	/**
	 * Parse the given file with the provided inputstream.
	 * 
	 * @param ins
	 *            Data stream
	 * @param len
	 *            Expected length of the stream
	 * @param parseMetadata
	 *            Whether to also parse metadata
	 * @return Result of the parse operation
	 * @throws TikaException
	 * @throws IOException
	 */
	public TikaResult parseFile(InputStream ins, int len, boolean parseMetadata) throws TikaException, IOException {

		Location loc = new Location();
		Map<String, String> fields = new HashMap<>();

		Metadata metadata = new Metadata();

		Optional<String> content = DocumentTikaParser.parse(ins, metadata, len);
		if (content.isPresent()) {
			log.debug("Got content {" + content.get() + "}");
		}

		if (parseMetadata) {
			Set<String> metadataWhitelist = options.getUploadOptions().getMetadataWhitelist();
			String[] metadataNames = metadata.names();
			for (String name : metadataNames) {
				String value = metadata.get(name);
				name = sanitizeName(name);

				if (metadataWhitelist != null) {
					if (!metadataWhitelist.contains(name)) {
						log.debug("Skipping entry {" + name + "} because it is absent in the whitelist.");
						continue;
					}
				} else if (skipSet.contains(name)) {
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
					if (name.equals("GPS_GPS_Altitude")) {
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
		} else {
			log.debug("Skipping setting meta data because it was explicitly disabled in the binary field schema.");
		}

		return new TikaResult(fields, content, loc);

	}

	private int getParserLimit(BinaryExtractOptions extractOptions, String contentType) {
		if (extractOptions != null && !extractOptions.getContent()) {
			return 0;
		} else {
			return getParserLimit(contentType);
		}
	}

	/**
	 * Return the parser limit for the given contentType.
	 * 
	 * @param contentType
	 * @return
	 */
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