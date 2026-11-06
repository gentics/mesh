package com.gentics.mesh.plugin.mustread.helper;

import com.gentics.mesh.plugin.mustread.MustReadPlugin;
import org.apache.commons.lang.StringUtils;

public class LogHelper {
    MustReadPlugin plugin;

    public LogHelper(MustReadPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Generate our default Heading for messages from this plugin and returns the full error message
     *
     * @param projectName
     * @param msg
     * @return
     */
    public String generateLogMessage(String projectName, String msg) {
        String builtMessage = String.format("%s ", getPluginName());
        if (!StringUtils.isBlank(projectName)) {
            builtMessage += String.format("(%s)", projectName);
        }

        builtMessage += String.format(": %s", msg);

        return builtMessage;
    }

    private String getPluginName() {
        return StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(plugin.getClass().getSimpleName()), " ");
    }
}
