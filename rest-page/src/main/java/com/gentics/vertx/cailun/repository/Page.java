package com.gentics.vertx.cailun.repository;

import java.util.Collection;
import java.util.HashSet;

import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
public class Page implements TagableContent {

	public Page() {
		addLabel("_SomeLabel");
		addLabel("Taggable");
	}

	@Labels
	private Collection<String> labels;

	@Indexed
	protected String name;

	@GraphId
	Long id;

	protected String filename;

	protected String teaser;

	protected String title;

	protected String author;

	protected String content;

	public String getName() {
		return name;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getTeaser() {
		return teaser;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getContent() {
		return this.content;
	}

	public void setTeaser(String text) {
		this.teaser = text;

	}

	public void addLabel(String label) {
		HashSet<String> newLabels;
		if (labels == null) {
			newLabels = new HashSet<>();
		} else {
			newLabels = new HashSet<>(this.labels);
		}
		if (newLabels.add(label)) {
			this.labels = newLabels;
		}
	}

	public Collection<String> getLabels() {
		return labels;
	}

	public void removeLabel(String label) {
		HashSet<String> newLabels = new HashSet<>(this.labels);
		if (newLabels.remove(label)) {
			this.labels = newLabels;
		}
	}

}
