package com.sinergise.sentinel.scihub.opensearch;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OpenSearchResult {
	@JsonProperty("feed")
	public OpenSearchResultFeed feed;

	public OpenSearchResultFeed getFeed() {
		return feed;
	}
}
