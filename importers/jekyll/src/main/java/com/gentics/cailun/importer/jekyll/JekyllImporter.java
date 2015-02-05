package com.gentics.cailun.importer.jekyll;

import org.yaml.snakeyaml.Yaml;

import com.gentics.cailun.importer.AbstractCaiLunImporter;

public class JekyllImporter extends AbstractCaiLunImporter {

	public static void main(String[] args) {
		Yaml yaml = new Yaml();
		Object obj = yaml.load("a: 1\nb: 2\nc:\n  - aaa\n  - bbb");
		System.out.println(obj);
	}
}
