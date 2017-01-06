package com.sinergise.sentinel.l1c.product.mapping;

import java.io.File;
import java.util.regex.Matcher;

import com.sinergise.sentinel.l1c.product.mapping.scihub.AbstractSciHubProductDatastrip;

public class S3ProductDatastrip {
	private File metadataFile;
	private File[] qiFiles;
	private File auxDir;
	private int index;
	private String Id;

	private File baseDirectory;

	

	public S3ProductDatastrip(AbstractSciHubProductDatastrip sciHubDatastrip, File s3ProductBase, int index) {
		Id = sciHubDatastrip.getId();
		baseDirectory = new File(s3ProductBase, "datastrip" + File.separator + index);
		metadataFile = new File(baseDirectory, "metadata.xml");
		File qiBase = new File(baseDirectory, "qi");

		qiFiles = new File[sciHubDatastrip.getQiFiles().length];
		for (int i = 0; i < qiFiles.length; i++) {
			File file = sciHubDatastrip.getQiFiles()[i];
			Matcher matcher = sciHubDatastrip.getQiReportFilePattern().matcher(file.getName());
			if (matcher.matches()) {
				qiFiles[i] = new File(qiBase, matcher.group(1)+"_report.xml");
			} else {
				throw new IllegalStateException("Unexpected QI file " + file.getName() + " in product" + s3ProductBase.getAbsolutePath()+"!");
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
