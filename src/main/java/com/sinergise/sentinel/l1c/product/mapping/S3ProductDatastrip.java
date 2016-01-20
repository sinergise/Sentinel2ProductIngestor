package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class S3ProductDatastrip {
	private File metadataFile;
	private File[] qiFiles;
	private File auxDir;
	private int index;
	private String Id;

	private File baseDirectory;

	private static final Pattern QI_REPORT_FILE_PATTERN = Pattern.compile("(.*)_(\\w+_\\w+_report\\.xml)$");

	public S3ProductDatastrip(SciHubProductDatastrip sciHubDatastrip, File s3ProductBase, int index) {
		Id = sciHubDatastrip.getId();
		baseDirectory = new File(s3ProductBase, "datastrip" + File.separator + index);
		metadataFile = new File(baseDirectory, "metadata.xml");
		File qiBase = new File(baseDirectory, "qi");

		qiFiles = new File[sciHubDatastrip.getQiFiles().length];
		for (int i = 0; i < qiFiles.length; i++) {
			File file = sciHubDatastrip.getQiFiles()[i];
			Matcher matcher = QI_REPORT_FILE_PATTERN.matcher(file.getName());
			if (matcher.matches()) {
				qiFiles[i] = new File(qiBase, matcher.group(2));
			} else {
				throw new IllegalStateException("Unexpected QI file " + file.getName() + "!");
			}
		}
		auxDir = new File(baseDirectory, "auxData");
	}

	public File getAuxDir() {
		return auxDir;
	}

	public File[] getQiFiles() {
		return qiFiles;
	}

	public File getMetadataFile() {
		return metadataFile;
	}

	public String getId() {
		return Id;
	}

	public int getIndex() {
		return index;
	}
	
	public File getBaseDirectory() {
		return baseDirectory;
	}
}
