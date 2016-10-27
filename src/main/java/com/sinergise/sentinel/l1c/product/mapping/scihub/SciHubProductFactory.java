package com.sinergise.sentinel.l1c.product.mapping.scihub;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sinergise.sentinel.scihub.SciHubEntry;

public class SciHubProductFactory {
	
	private static final Pattern PRODUCT_NAME = Pattern
			.compile("^S2[AB]_(OPER_PRD_)?MSIL1C.*\\.SAFE$");
	
	public static AbstractSciHubProduct loadProduct(File productFolder, SciHubEntry sciHubEntry) {
		String productName = productFolder.getName();
		Matcher matcher = PRODUCT_NAME.matcher(productName);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unsupported product name: "+productName);
		}
		if (matcher.group(1)==null) {
			return new com.sinergise.sentinel.l1c.product.mapping.scihub.v2.SciHubProduct(productFolder, sciHubEntry);
		} else {
			return new com.sinergise.sentinel.l1c.product.mapping.scihub.v1.SciHubProduct(productFolder, sciHubEntry);
		}
	}
}
