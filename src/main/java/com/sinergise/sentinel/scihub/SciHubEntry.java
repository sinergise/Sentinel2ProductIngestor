package com.sinergise.sentinel.scihub;

import java.io.File;
import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.l1c.product.mapping.SciHubProduct;
import com.sinergise.sentinel.scihub.opensearch.Link;
import com.sinergise.sentinel.scihub.opensearch.OpenSearchEntry;

public class SciHubEntry {
	
	private static final Logger logger = LoggerFactory.getLogger(SciHubEntry.class);
	
	private String id;
	private String name;

	private String contentUrl;
	
	private File archiveFile;
	
	private Date productStartTime;
	private Date productStopTime;
	private Date ingestionDate;
	private static final Pattern NAME_DATES_PATTERN = Pattern.compile("^.*_V([0-9T]{15})_([0-9T]{15})$");
	
	public SciHubEntry(String id, String name) {
		this.name=name;
		this.id=id;
	}
	
	public SciHubEntry(OpenSearchEntry osEntryDto) {
		this.id = osEntryDto.getId();
		this.name = osEntryDto.getTitle();
		
		
		Matcher mDate = NAME_DATES_PATTERN.matcher(name);
		if (!mDate.matches()) {
			throw new IllegalStateException("Can't extract product times from name! "+name);
		}
		
		try {
			productStartTime = SciHubProduct.FILE_DATE_FORMAT.parse(mDate.group(1));
			productStopTime = SciHubProduct.FILE_DATE_FORMAT.parse(mDate.group(2));
		} catch (ParseException ex) {
			throw new IllegalStateException("Can't extract product times from name! ",ex);
		}
		
		for (Link linkDto: osEntryDto.getLinks()) {
			if (linkDto.getRelation()==null) {
				contentUrl = linkDto.getHref();
			}
		}
		
		
		ingestionDate = osEntryDto.getIngestionDate();
		
		if (contentUrl == null) {
			logger.error("IngestorEntry {} does not provide content URL!", this);
			throw new IllegalStateException("No content url found!");
		}
	}
	
	public String getName() {
		return name;
	}
	
	public File getArchiveFile() {
		return archiveFile;
	}
	
	public void setArchiveFile(File archiveFile) {
		this.archiveFile = archiveFile;
	}

	public String getId() {
		return id;
	}

	public String getContentUrl() {
		return contentUrl;
	}
	
	public Date getIngestionDate() {
		return ingestionDate;
	}
	
	public Date getProductStartTime() {
		return productStartTime;
	}
	
	public Date getProductStopTime() {
		return productStopTime;
	}

	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SciHubEntry other = (SciHubEntry) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}
	
	
	
}
