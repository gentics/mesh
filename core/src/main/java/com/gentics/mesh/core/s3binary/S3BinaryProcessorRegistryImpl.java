package com.gentics.mesh.core.s3binary;

import com.gentics.mesh.core.s3binary.impl.S3BasicImageDataProcessor;
import com.gentics.mesh.core.s3binary.impl.TikaS3BinaryProcessor;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry which holds all S3 binary data processors.
 */
@Singleton
public class S3BinaryProcessorRegistryImpl implements S3BinaryProcessorRegistry {

	private List<S3BinaryDataProcessor> processors = new ArrayList<>();

	@Inject
	public S3BinaryProcessorRegistryImpl(MeshOptions options, S3BasicImageDataProcessor imageProcessor,
                                         TikaS3BinaryProcessor tikaProcessor) {
		MeshUploadOptions uploadOptions = options.getUploadOptions();

		// Add built-in processors
		addProcessor(imageProcessor);
		addProcessor(tikaProcessor);
	}

	/**
	 * Add the processor to the list of processors.
	 * 
	 * @param processor
	 */
	public void addProcessor(S3BinaryDataProcessor processor) {
		processors.add(processor);
	}

	/**
	 * Return the list of registered processors.
	 * 
	 * @param contentType
	 * @return
	 */
	public List<S3BinaryDataProcessor> getProcessors(String contentType) {
		return processors.stream()
			.filter(p -> p.accepts(contentType))
			.collect(Collectors.toList());
	}
}
