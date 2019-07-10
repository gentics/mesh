package com.gentics.mesh.plugin.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.pf4j.Plugin;
import org.pf4j.PluginDependency;

import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.plugin.util.PluginUtils;

public class MeshPluginDescriptorImpl implements MeshPluginDescriptor {

	private String pluginId;
	private String name;
	private String pluginDescription;
	private String pluginClass = Plugin.class.getName();
	private String version;
	private String requires = "*"; // SemVer format
	private String author;
	private List<PluginDependency> dependencies;
	private String license;
	private String inception;

	public MeshPluginDescriptorImpl() {
		dependencies = new ArrayList<>();
	}

	public MeshPluginDescriptorImpl(String pluginId, String name, String pluginDescription, String pluginClass, String version,
		String requires, String author,
		String license, String inception) {
		this();
		this.pluginId = pluginId;
		this.name = name;
		this.pluginDescription = pluginDescription;
		this.pluginClass = pluginClass;
		this.version = version;
		this.requires = requires;
		this.author = author;
		this.license = license;
		this.inception = inception;
	}

	public MeshPluginDescriptorImpl(String pluginId, Class<?> clazz, PluginManifest manifest) {
		this(pluginId, manifest.getName(), manifest.getDescription(), clazz.getName(), manifest.getVersion(), "",
			manifest.getAuthor(),
			manifest.getLicense(), manifest.getInception());
	}

	public MeshPluginDescriptorImpl(String pluginId, Class<?> clazz) {
		this(pluginId, clazz.getName(), "", clazz.getName(), "", "", "", "", "");
	}

	public void addDependency(PluginDependency dependency) {
		this.dependencies.add(dependency);
	}

	/**
	 * Returns the unique identifier of this plugin.
	 */
	@Override
	public String getPluginId() {
		return pluginId;
	}

	/**
	 * Returns the description of this plugin.
	 */
	@Override
	public String getPluginDescription() {
		return pluginDescription;
	}

	/**
	 * Returns the name of the class that implements Plugin interface.
	 */
	@Override
	public String getPluginClass() {
		return pluginClass;
	}

	/**
	 * Returns the version of this plugin.
	 */
	@Override
	public String getVersion() {
		return version;
	}

	/**
	 * Returns string version of requires
	 *
	 * @return String with requires expression on SemVer format
	 */
	@Override
	public String getRequires() {
		return requires;
	}

	/**
	 * Returns the provider name of this plugin.
	 */
	@Override
	public String getProvider() {
		return author;
	}

	@Override
	public String getAuthor() {
		return author;
	}

	@Override
	public String getName() {
		return name;
	}

	/**
	 * Returns the legal license of this plugin, e.g. "Apache-2.0", "MIT" etc
	 */
	@Override
	public String getLicense() {
		return license;
	}

	/**
	 * Returns all dependencies declared by this plugin. Returns an empty array if this plugin does not declare any require.
	 */
	@Override
	public List<PluginDependency> getDependencies() {
		return dependencies;
	}

	@Override
	public String toString() {
		return "PluginDescriptor [pluginId=" + pluginId + ", pluginClass="
			+ pluginClass + ", version=" + version + ", author="
			+ author + ", dependencies=" + dependencies + ", description="
			+ pluginDescription + ", requires=" + requires + ", license="
			+ license + ", inception=" + inception + "]";
	}

	protected MeshPluginDescriptor setPluginId(String pluginId) {
		this.pluginId = pluginId;
		return this;
	}

	protected MeshPluginDescriptor setPluginDescription(String pluginDescription) {
		this.pluginDescription = pluginDescription;
		return this;
	}

	protected MeshPluginDescriptor setPluginClass(String pluginClassName) {
		this.pluginClass = pluginClassName;
		return this;
	}

	protected MeshPluginDescriptor setPluginVersion(String version) {
		this.version = version;
		return this;
	}

	protected MeshPluginDescriptor setAuthor(String author) {
		this.author = author;
		return this;
	}

	protected MeshPluginDescriptor setRequires(String requires) {
		this.requires = requires;
		return this;
	}

	protected MeshPluginDescriptor setDependencies(String dependencies) {
		if (dependencies != null) {
			dependencies = dependencies.trim();
			if (dependencies.isEmpty()) {
				this.dependencies = Collections.emptyList();
			} else {
				this.dependencies = new ArrayList<>();
				String[] tokens = dependencies.split(",");
				for (String dependency : tokens) {
					dependency = dependency.trim();
					if (!dependency.isEmpty()) {
						this.dependencies.add(new PluginDependency(dependency));
					}
				}
				if (this.dependencies.isEmpty()) {
					this.dependencies = Collections.emptyList();
				}
			}
		} else {
			this.dependencies = Collections.emptyList();
		}

		return this;
	}

	public MeshPluginDescriptor setLicense(String license) {
		this.license = license;
		return this;
	}

	@Override
	public String getInception() {
		return inception;
	}

	protected MeshPluginDescriptor setInception(String inception) {
		this.inception = inception;
		return this;
	}

	@Override
	public PluginManifest toPluginManifest() {
		PluginManifest manifest = new PluginManifest();
		manifest.setName(getPluginId());
		manifest.setDescription(getPluginDescription());
		manifest.setLicense(getLicense());
		manifest.setVersion(getVersion());
		manifest.setInception(getInception());
		manifest.setAuthor(getAuthor());
		return manifest;
	}

}
