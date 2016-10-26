package com.sinergise.sentinel.l1c.product.mapping.scihub.v1;

import java.io.File;
import java.io.FileFilter;

public class FileSuffixFilter implements FileFilter {
	public static FileSuffixFilter xml() {
		return new FileSuffixFilter(new String[] { ".xml" });
	}

	public static FileSuffixFilter jp2() {
		return new FileSuffixFilter(new String[] { ".jp2" });
	}

	private final String[] suffixes;

	public FileSuffixFilter(String[] suffixes) {
		this.suffixes = suffixes;
	}

	@Override
	public boolean accept(File pathname) {
		if (pathname.isFile()) {
			for (String suffix : suffixes) {
				if (pathname.getName().toLowerCase().endsWith(suffix)) {
					return true;
				}
			}
		}
		return false;
	}
}