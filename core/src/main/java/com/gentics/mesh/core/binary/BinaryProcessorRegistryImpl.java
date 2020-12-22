package com.gentics.mesh.core.binary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.impl.BasicImageDataProcessor;
import com.gentics.mesh.core.binary.impl.TikaBinaryProcessor;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.MeshUploadOptions;

/**
 * Registry which holds all binary data processors.
 */
@Singleton
public class BinaryProcessorRegistryImpl implements BinaryProcessorRegistry {

	private List<BinaryDataProcessor> processors = new ArrayList<>();

	@Inject
	public BinaryProcessorRegistryImpl(MeshOptions options, BasicImageDataProcessor imageProcessor,
		TikaBinaryProcessor tikaProcessor) {
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
	public void addProcessor(BinaryDataProcessor processor) {
		processors.add(processor);
	}

	/**
	 * Return the list of registered processors.
	 * 
	 * @param contentType
	 * @return
	 */
	public List<BinaryDataProcessor> getProcessors(String contentType) {
		return processors.stream()
			.filter(p -> p.accepts(contentType))
			.collect(Collectors.toList());
	}
}
