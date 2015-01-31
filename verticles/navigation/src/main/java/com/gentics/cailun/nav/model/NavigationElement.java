package com.gentics.cailun.nav.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NavigationElement {

	private NavigationElementType type;

	private List<NavigationElement> children = new ArrayList<NavigationElement>();

	private String name;

	private String path;

	private String target = new String("#");
}
