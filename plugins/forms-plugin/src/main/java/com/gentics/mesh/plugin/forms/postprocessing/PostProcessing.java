package com.gentics.mesh.plugin.forms.postprocessing;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an implementation of {@link PostProcessingAction} and declares its unique key.
 * The key is used to configure optional properties for the action in the plugin configuration
 * (<code>postProcessing.actions.&lt;key&gt;</code>) and to track the status of the action for posted form data.
 *
 * <p>Annotated classes must be located in the package {@code com.gentics.mesh.plugin.forms.postprocessing},
 * which is scanned once when the plugin is started.
 *
 * <p>Example:
 * <pre>
 *   &#64;PostProcessing("crm-export")
 *   public class CrmExportAction implements PostProcessingAction { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PostProcessing {
	/**
	 * Unique key of the post processing action
	 */
	String value();
}
