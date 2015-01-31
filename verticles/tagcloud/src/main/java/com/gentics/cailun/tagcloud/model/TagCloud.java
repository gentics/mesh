package com.gentics.cailun.tagcloud.model;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TagCloud {

	public Set<TagCloudEntry> entries = new HashSet<>();

}
