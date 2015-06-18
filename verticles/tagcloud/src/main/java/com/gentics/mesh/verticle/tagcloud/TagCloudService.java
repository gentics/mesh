package com.gentics.mesh.verticle.tagcloud;

import java.util.List;

import com.gentics.mesh.tagcloud.model.TagCloudResult;


public class TagCloudService {

	public List<TagCloudResult> getTagCloudInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Return the count of relationships from all tags to pages
	 */
	//	@Query("MATCH (n:Tag)<-[r:TAGGED]-(x:Page) RETURN n as tag, COUNT(r) as count ORDER BY COUNT(r) DESC")
	//	public List<TagCloudResult> getTagCloudInfo();

}
