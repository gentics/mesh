package com.gentics.mesh.plugin.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.pf4j.Plugin;
import org.pf4j.PluginDependency;

import com.gentics.mesh.plugin.MeshPluginDescriptor;
import com.gentics.mesh.plugin.PluginManifest;

/**
 * POJO which describes the metadata of a plugin.
 */
public class MeshPluginDescriptorImpl implements MeshPluginDescriptor {

	private String id;
	private String name;
	private String description;
	private String pluginClass = Plugin.class.getName();
	private String version;
	private String requires = "*"; // SemVer format
	private String author;
	private List<PluginDependency> dependencies;
	private String license;
	private String inception;

	public MeshPluginDescriptorImpl() {
		this.dependencies = new ArrayList<>();
	}

	public MeshPluginDescriptorImpl(String id, String name, String pluginDescription, String pluginClass, String version,
		String requires, String author,
		String license, String inception) {
		this();
		this.id = id;
		this.name = name;
		this.description = pluginDescription;
		this.pluginClass = pluginClass;
		this.version = version;
		this.requires = requires;
		this.author = author;
		this.license = license;
		this.inception = inception;
	}

	public MeshPluginDescriptorImpl(Class<?> clazz, PluginManifest manifest) {
		this(manifest.getId(), manifest.getName(), manifest.getDescription(), clazz.getName(), manifest.getVersion(), "",
			manifest.getAuthor(),
			manifest.getLicense(), manifest.getInception());
	}

	public MeshPluginDescriptorImpl(Class<?> clazz, String id) {
		this(id, clazz.getSimpleName(), "NA", clazz.getName(), "0.0.1-SNAPSHOT", "", "Unknown Author", "Unknown License", today());
	}

	private static String today() {
		String pattern = "yyyy-MM-dd";
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
		String date = simpleDateFormat.format(new Date());
		return date;
	}

	public void addDependency(PluginDependency dependency) {
		this.dependencies.add(dependency);
	}

	@Override
	public String getPluginId() {
		return id;
	}

	/**
	 * Returns the description of this plugin.
	 */
	@Override
	public String getPluginDescription() {
		return description;
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
		return "PluginDescriptor [id=" + id + ", pluginClass="
			+ pluginClass + ", version=" + version + ", author="
			+ author + ", dependencies=" + dependencies + ", description="
			+ description + ", requires=" + requires + ", license="
			+ license + ", inception=" + inception + "]";
	}

	protected MeshPluginDescriptor setPluginId(String pluginId) {
		this.id = pluginId;
		return this;
	}

	protected MeshPluginDescriptor setPluginName(String pluginName) {
		this.name = pluginName;
		return this;
	}

	protected MeshPluginDescriptor setPluginDescription(String pluginDescription) {
		this.description = pluginDescription;
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
		manifest.setId(getId());
		manifest.setName(getName());
		manifest.setDescription(getPluginDescription());
		manifest.setLicense(getLicense());
		manifest.setVersion(getVersion());
		manifest.setInception(getInception());
		manifest.setAuthor(getAuthor());
		return manifest;
	}

}
