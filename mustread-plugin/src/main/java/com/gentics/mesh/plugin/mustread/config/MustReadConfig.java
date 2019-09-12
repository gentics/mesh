package com.gentics.mesh.plugin.mustread.config;

import java.util.List;

public class MustReadConfig {
    private List<ProjectConfig> projects;

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

}
