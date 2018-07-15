package com.gentics.mesh.core.binary;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.binary.impl.BasicImageDataProcessor;
import com.gentics.mesh.core.binary.impl.TikaBinaryProcessor;

/**
 * Registry which holds all binary data processors.
 */
@Singleton
public class BinaryProcessorRegistry {

	private List<BinaryDataProcessor> processors = new ArrayList<>();

	@Inject
	public BinaryProcessorRegistry(BasicImageDataProcessor imageProcessor, TikaBinaryProcessor tikaProcessor) {
		// Add build-in processors
		addProcesor(imageProcessor);
		addProcesor(tikaProcessor);
	}

	private void addProcesor(BinaryDataProcessor processor) {
		processors.add(processor);
	}

	public List<BinaryDataProcessor> getProcessors(String contentType) {
		return processors.stream()
			.filter(p -> p.accepts(contentType))
			.collect(Collectors.toList());
	}
}
