package com.sinergise.sentinel.scihub.opensearch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Link {
	@JsonProperty("rel")
	private String relation;
	@JsonProperty("href")
	private String href;
	@JsonProperty("type")
	private String type;

	public String getRelation() {
		return relation;
	}

	public String getHref() {
		return href;
	}

	public String getType() {
		return type;
	}

}