package com.gentics.mesh.plugin.common;

import org.apache.commons.lang3.StringUtils;

import java.util.stream.Stream;

/**
 * Wrapper for Handlebars template source or file path.
 */
public class TemplateWrapper {

	/** The wrapped value (either a template source, or the path to a file). */
	private final String value;

	/** Whether the wrapped value is a path to a template file. */
	private final boolean isTemplatePath;

	protected TemplateWrapper(String value, boolean isTemplatePath) {
		this.value = value;
		this.isTemplatePath = isTemplatePath;
	}

	/**
	 * Create a new instance.
	 *
	 * <p>
	 *     When {@code source} is not empty, {@link #isTemplatePath()} will
	 *     return {@code false} and {@link #value()} will return the
	 *     template source itself.
	 * </p>
	 *
	 * <p>
	 *     When {@code source} is empty, {@link #isTemplatePath()} will return
	 *     {@code true} and {@link #value()} will return the <em>first</em>
	 *     non-empty entry from the given {@code paths}.
	 * </p>
	 *
	 * <p>
	 *     When {@code source} and all entries in {@code paths} are empty
	 *     {@link #isValid()} will return {@code false} and {@link #value()}
	 *     will return {@code null}.
	 * </p>
	 *
	 * <p>
	 *     <strong>NOTE</strong> that no checks are performed if the given
	 *     paths are actually valid file paths.
	 * </p>
	 *
	 * @param source The template source.
	 * @param paths The template paths in descending priority.
	 * @return A new instance of {@code TemplateWrapper}.
	 */
	public static TemplateWrapper wrap(String source, String ...paths) {
		if (StringUtils.isNotBlank(source)) {
			return new TemplateWrapper(source, false);
		}

		return new TemplateWrapper(
			Stream.of(paths).filter(StringUtils::isNotBlank).findFirst().orElse(null),
			true);
	}

	/**
	 * Checks whether the wrapped value is not {@code null}.
	 *
	 * <p>
	 *     <strong>NOTE</strong> If the wrapped value is a file path, it is
	 *     <em>not</em> checked if it is a valid file path that can be read.
	 * </p>
	 *
	 * @return {@code true} when a wrapped value is present.
	 */
	public boolean isValid() {
		return value != null;
	}

	/**
	 * Whether the wrapped value is a path to a template file.
	 *
	 * @return {@code true} when the wrapped value is the path to a template file.
	 */
	public boolean isTemplatePath() {
		return isTemplatePath;
	}

	/**
	 * Get the wrapped value.
	 *
	 * @return The wrapped value.
	 */
	public String value() {
		return value;
	}
}
