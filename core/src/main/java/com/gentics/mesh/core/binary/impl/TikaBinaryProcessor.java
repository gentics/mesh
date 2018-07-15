package com.gentics.mesh.core.binary.impl;

import java.io.File;
import java.io.FileInputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;

import com.gentics.mesh.core.binary.AbstractBinaryProcessor;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.handler.ActionContext;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;

@Singleton
public class TikaBinaryProcessor extends AbstractBinaryProcessor {

	private static final Logger log = LoggerFactory.getLogger(TikaBinaryProcessor.class);

	@Inject
	public TikaBinaryProcessor() {
	}

	@Override
	public boolean accepts(String contentType) {
		return false;
	}

	@Override
	public Completable process(ActionContext ac, FileUpload upload, BinaryGraphField field) {
		return Completable.defer(() -> {

			File uploadFile = new File(upload.uploadedFileName());

			Parser parser = new AutoDetectParser();
			BodyContentHandler handler = new BodyContentHandler(-1);
			Metadata metadata = new Metadata();
			FileInputStream inputstream = new FileInputStream(uploadFile);
			ParseContext context = new ParseContext();

			parser.parse(inputstream, handler, metadata, context);
			if (log.isDebugEnabled()) {
				log.debug("Parsed file {" + uploadFile + "} got content: {" + handler.toString() + "}");
			}

			if (log.isDebugEnabled()) {
				log.debug("Metadata of the PDF:");
				String[] metadataNames = metadata.names();

				for (String name : metadataNames) {
					log.debug("{" + name + "}={" + metadata.get(name) + "}");
				}
			}
			return Completable.complete();
		});
	}

}
