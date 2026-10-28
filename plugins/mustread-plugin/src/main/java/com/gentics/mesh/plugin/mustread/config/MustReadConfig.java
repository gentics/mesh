package com.gentics.mesh.plugin.mustread.config;

import com.gentics.mesh.plugin.common.license.LicenseConfiguration;
import org.apache.commons.lang.StringUtils;

import java.util.List;

public class MustReadConfig extends LicenseConfiguration {
    private List<ProjectConfig> projects;

    private String nodeListParameterName;

    public List<ProjectConfig> getProjects() {
        return projects;
    }

    public MustReadConfig setProjects(List<ProjectConfig> projects) {
        this.projects = projects;

        return this;
    }

    public ProjectConfig getProjectConfiguration(String name) {
        for (ProjectConfig project : getProjects()) {
            if (project.isConfigurationForProject(name)) {
                return project;
            }
        }

        return null;
    }

    public String getNodeListParameterName() {
        if (StringUtils.isBlank(nodeListParameterName)) {
            return "uuids";
        }
        return nodeListParameterName;
    }

    public MustReadConfig setNodeListParameterName(String nodeListParameterName) {
        this.nodeListParameterName = nodeListParameterName;

        return this;
    }
}
