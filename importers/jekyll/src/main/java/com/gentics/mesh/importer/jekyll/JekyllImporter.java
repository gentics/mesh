package com.gentics.mesh.importer.jekyll;

import org.yaml.snakeyaml.Yaml;

import com.gentics.mesh.importer.AbstractMeshImporter;

public class JekyllImporter extends AbstractMeshImporter {

	public static void main(String[] args) {
		Yaml yaml = new Yaml();
		Object obj = yaml.load("a: 1\nb: 2\nc:\n  - aaa\n  - bbb");
		System.out.println(obj);
	}
}
