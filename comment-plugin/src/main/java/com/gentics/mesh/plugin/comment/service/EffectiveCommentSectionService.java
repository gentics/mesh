package com.gentics.mesh.plugin.comment.service;

import com.gentics.mesh.plugin.comment.config.Configuration;
import com.gentics.mesh.plugin.comment.model.CommentSection;
import com.gentics.mesh.plugin.comment.model.EffectiveCommentSection;

import javax.inject.Inject;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class EffectiveCommentSectionService {
	private final Configuration configuration;

	@Inject
	public EffectiveCommentSectionService(Configuration configuration) {
		this.configuration = configuration;
	}

	public EffectiveCommentSection fromSection(CommentSection section) {
		EffectiveCommentSection newSection = new EffectiveCommentSection(section.getDelegate());
		newSection.setAllowAnonymous(getConfig(section.isAllowAnonymous(), Configuration::isAllowAnonymous));
		newSection.setRequiresApproval(getConfig(section.isRequiresApproval(), Configuration::isRequiresApproval));
		newSection.setModeratorEmails(getConfig(section.getModeratorEmails(), Configuration::getModeratorEmails));
		newSection.setRequiredForAnonymous(getConfig(section.getRequiredForAnonymous(), Configuration::getRequiredForAnonymous));
		newSection.setNodeConfigField(getConfig(section.getNodeConfigField(), Configuration::getNodeConfigField));
		newSection.setNode(section.getNode());

		return newSection;
	}

	/**
	 * Gets the first non-null value for a field. This is useful for the configuration fallback which
	 * goes like this: CommentSection > ProjectConfiguration > GlobalConfiguration
	 *
	 * @param sectionValue Getter of the {@link CommentSection}
	 * @param configMapper Getter of the {@link Configuration}
	 */
	private <T> T getConfig(Optional<T> sectionValue, Function<Configuration, T> configMapper) {
		return coalesceOptional(
			sectionValue,
			Optional.of(configuration).map(configMapper)
		).orElse(null);
	}

	/**
	 * Returns the value of the first non-empty optional.
	 * If all arguments are empty, an empty optional is returned.
	 */
	private <T> Optional<T> coalesceOptional(Optional<T>... opts) {
		return Stream.of(opts)
			.flatMap(this::toStream)
			.findAny();
	}

	/**
	 * Flattens an optional to a stream.
	 */
	private <T> Stream<T> toStream(Optional<T> opt) {
		return opt.map(Stream::of).orElseGet(Stream::empty);
	}
}
