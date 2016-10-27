package com.sinergise.sentinel.l1c.product.mapping.scihub.v1;

import java.io.File;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProduct;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductTile;

public class SciHubProductTile extends AbstractSciHubProductTile {
	
	public SciHubProductTile(AbstractSciHubProduct sciHubProduct, File productTileDir) {
		super(sciHubProduct, productTileDir);
	}
	public static final Logger logger = LoggerFactory.getLogger(SciHubProductTile.class);
		
	private static final Pattern BAND_TILE_PATTERN = Pattern
			.compile("^.*_T([0-9]{2})([A-Z]{3}).*_(B[0-9][A-Z0-9]\\.jp2).*$");

	private static final Pattern QI_REPORT_FILE_PATTERN = Pattern.compile("(.*)_(\\w+_\\w+_report\\.xml)$");
	private static final Pattern QI_MASK_FILE_PATTERN = Pattern
			.compile("^.*_(MSK_[A-Z]+).*(_B[0-9][A-Z0-9]).*(\\.gml)$");
	private static final Pattern AUX_FILE_PATTERN = Pattern.compile("^.*_AUX_([A-Z]+).*$");

	
	public Pattern getJp2BandFilePattern() {
		return BAND_TILE_PATTERN;
	}

	
	public Pattern getQiReportFilePattern() {
		return QI_REPORT_FILE_PATTERN;
	}

	
	public Pattern getMaskGMLFilePattern() {
		return QI_MASK_FILE_PATTERN;
	}

	
	public Pattern getAuxFilePattern() {
		return AUX_FILE_PATTERN;
	}	

}
