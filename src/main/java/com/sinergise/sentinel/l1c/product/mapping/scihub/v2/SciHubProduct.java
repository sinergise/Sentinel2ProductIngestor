package com.sinergise.sentinel.l1c.product.mapping.scihub.v2;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProduct;
import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductTile;
import com.sinergise.sentinel.scihub.SciHubEntry;

public class SciHubProduct extends AbstractSciHubProduct {
	private static final Pattern DATASTRIP_ID_PATTERN = Pattern
			.compile("^(S2A_OPER_MSI_L1C_)?(.*)(_.*)$");

	public SciHubProduct(File productBase, SciHubEntry sciHubEntry) {
		super(productBase, sciHubEntry, id -> {
			Matcher m = DATASTRIP_ID_PATTERN.matcher(id);
			if (!m.matches())
				throw new IllegalArgumentException("Unknown datastripId '"+id+"' pattern!");
			return m.group(2);
		});
	}
		
	@Override
	protected File getMetadataFile(File productBase) {
		return new File(productBase, "MTD_MSIL1C.xml");
	}

	@Override
	protected AbstractSciHubProductTile createTile(AbstractSciHubProduct sciHubProduct, File tileDir) {
		return new SciHubProductTile(sciHubProduct, tileDir);
	}

}
