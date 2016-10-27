package com.sinergise.sentinel.l1c.product.mapping.scihub.v2;

import java.io.File;
import java.util.regex.Pattern;

import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProduct;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductTile;

public class SciHubProductTile extends AbstractSciHubProductTile {
	private static final Pattern BAND_TILE_PATTERN = Pattern
			.compile("^T([0-9]{2})([A-Z]{3}).*_(B[0-9][A-Z0-9]\\.jp2|TCI\\.jp2).*$");
	private static final Pattern QI_REPORT_FILE_PATTERN = Pattern
			.compile("(.*)_(\\w+_\\w+_report\\.xml)$");
	private static final Pattern QI_MASK_FILE_PATTERN = Pattern
			.compile("^(MSK_[A-Z]+).*(_B[0-9][A-Z0-9]).*(\\.gml)$");
	private static final Pattern AUX_FILE_PATTERN = Pattern
			.compile("^AUX_([A-Z]+).*$");

	
	public SciHubProductTile(AbstractSciHubProduct sciHubProduct, File productTileDir) {
		super(sciHubProduct, productTileDir);
	}

	@Override
	public Pattern getJp2BandFilePattern() {
		return BAND_TILE_PATTERN;
	}

	@Override
	public Pattern getQiReportFilePattern() {
		return QI_REPORT_FILE_PATTERN;
	}

	@Override
	public Pattern getMaskGMLFilePattern() {
		return QI_MASK_FILE_PATTERN;
	}

	@Override
	public Pattern getAuxFilePattern() {
		return AUX_FILE_PATTERN;
	}

}
