package com.gentics.mesh.changelog.highlevel.change;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.binary.impl.TikaBinaryProcessor;
import com.gentics.mesh.core.binary.impl.TikaResult;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.storage.LocalBinaryStorageImpl;
import com.syncleus.ferma.FramedTransactionalGraph;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Changelog entry which re-runs the tika extraction.
 */
@Singleton
public class ExtractPlainText extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(ExtractPlainText.class);

	private final TikaBinaryProcessor processor;

	private final Lazy<LocalBinaryStorageImpl> storage;

	private final Lazy<BootstrapInitializer> boot;

	private final Binaries binaries;

	@Inject
	public ExtractPlainText(Lazy<BootstrapInitializer> boot, TikaBinaryProcessor processor, Lazy<LocalBinaryStorageImpl> storage, Binaries binaries) {
		this.boot = boot;
		this.processor = processor;
		this.storage = storage;
		this.binaries = binaries;
	}

	@Override
	public String getUuid() {
		return "C2087EA6912148C4887EA6912188C417";
	}

	@Override
	public String getName() {
		return "Extract Plain Text";
	}

	@Override
	public void apply() {
		log.info("Applying change: " + getName());
		FramedTransactionalGraph graph = Tx.getActive().getGraph();
		AtomicLong total = new AtomicLong(0);
		BinaryDaoWrapper binaryDao = Tx.get().binaryDao();
		binaryDao.findAll().runInExistingTx(Tx.get()).forEach(binary -> {
			final String filename = storage.get().getFilePath(binary.getUuid());
			File uploadFile = new File(filename);
			if (uploadFile.exists()) {

				Result<? extends BinaryGraphField> fields = binaryDao.findFields(binary);
				Map<String, TikaResult> results = new HashMap<>();

				fields.forEach(field -> {
					String contentType = field.getMimeType();
					if (!results.containsKey(contentType)) {
						int limit = processor.getParserLimit(contentType);
						try (FileInputStream ins = new FileInputStream(uploadFile)) {
							TikaResult result = processor.parseFile(ins, limit);
							if (log.isDebugEnabled()) {
								log.debug("Parsing file {" + uploadFile + "} - {" + contentType + "}");
							}
							results.put(contentType, result);
						} catch (Exception e) {
							log.error("Error while parsing file {" + uploadFile + "}", e);
						}
					}
					TikaResult res = results.get(contentType);
					if (res != null) {
						Optional<String> plainText = res.getPlainText();
						if (plainText.isPresent()) {
							field.setPlainText(plainText.get());
						}
					}
					if (total.get() % 10 == 0) {
						log.info("Updated {" + total + "} fields.");
					}

					if (total.get() % 100 == 0) {
						graph.commit();
					}
					total.incrementAndGet();

				});
			} else {
				graph.commit();
				log.info("File for binary {" + binary.getUuid() + "} could not be found {" + filename + "}");
			}
		});
		log.info("Done updating {" + total + "} binary fields.");

	}

	@Override
	public String getDescription() {
		return "Parses the uploads and extracts the plain text";
	}

	@Override
	public boolean isAllowedInCluster(MeshOptions options) {
		return false;
	}
}
