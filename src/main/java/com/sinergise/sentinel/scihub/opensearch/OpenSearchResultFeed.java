package com.sinergise.sentinel.scihub.opensearch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class OpenSearchResultFeed {
	@JsonProperty("opensearch:startIndex")
	long startIndex;
	
	@JsonProperty("opensearch:totalResults")
	long totalResultsCount;
	
	@JsonProperty("id")
	String id;

	@JsonProperty("link")
	List<Link> links;
	
	@JsonProperty("entry")
	List<OpenSearchEntry> entries;
	
	public List<OpenSearchEntry> getEntries() {
		return entries;
	}
	
	public List<Link> getLinks() {
		return links;
	}
	
	
	public Link getLink(String rel) {
		if (links == null) return null;
		for (Link link:links) {
			if (rel.equals(link.getRelation()))
				return link;
		}
		return null;
	}
}
