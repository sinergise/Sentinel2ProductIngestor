package com.sinergise.sentinel.scihub;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.l1c.product.L1CProductConstants;
import com.sinergise.sentinel.scihub.opensearch.Link;
import com.sinergise.sentinel.scihub.opensearch.OpenSearchEntry;

public class SciHubEntry {
	
	private static final Logger logger = LoggerFactory.getLogger(SciHubEntry.class);
	
	private String id;
	private String name;

	private String contentUrl;
	
	private File archiveFile;
	
	private Date productTime;
	private Date ingestionDate;
	private static final Pattern SAFE_NAME_DATES_PATTERN = Pattern.compile("^.*_V([0-9T]{15})_([0-9T]{15})$");
	private static final Pattern SAFECOMPACT_NAME_DATES_PATTERN = Pattern.compile("^S2[AB]_MSIL1C_([0-9T]{15})_.*$");
	
	
	
	public SciHubEntry(String id, String name) {
		this.name=name;
		this.id=id;
	}
	
	public SciHubEntry(OpenSearchEntry osEntryDto) {
		this.id = osEntryDto.getId();
		this.name = osEntryDto.getTitle();
		
		
		SimpleDateFormat filenameDateFormat = L1CProductConstants.getFilenameDateFormat();
		Matcher safeDateMatcher = SAFE_NAME_DATES_PATTERN.matcher(name);
		Matcher safeCompactDateMatcher = SAFECOMPACT_NAME_DATES_PATTERN.matcher(name);
		if (safeDateMatcher.matches()) {
			try {
				productTime = filenameDateFormat.parse(safeDateMatcher.group(2));
			} catch (ParseException ex) {
				throw new IllegalStateException("Can't extract product time from name! ",ex);
			}
		} else if (safeCompactDateMatcher.matches()) {
			try {
				productTime = filenameDateFormat.parse(safeCompactDateMatcher.group(1));
			} catch (ParseException ex) {
				throw new IllegalStateException("Can't extract product time from name! ",ex);
			}
		} else { 
			throw new IllegalStateException("Can't extract product times from name! "+name);
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
	
	public Date getProductTime() {
		return productTime;
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
