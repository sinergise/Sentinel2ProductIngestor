Sentinel2ProductIngestor
================

# About 
Sentinel2ProductIngestor is used to download earth observation data provided by the http://www.esa.int/Our_Activities/Observing_the_Earth/Copernicus/Sentinel-2 satellites and store it to Amazon S3.
At the moment there is not much else here than application pulling data from Sentinel Science Hub. Lots of effort was put into solving technical difficulties (e.g. Hub not working, transfer being interrupted, etc.) and ensuring reliability of the transferred data. 
In the future we plan to add some further utils related to organisation of Sentinel archive.


# About the data
## Sentinel 2 data structure on S3
Data structure is described here: http://sentinel-pds.s3-website.eu-central-1.amazonaws.com/
Check also original MSI specifications by ESA (https://sentinel.esa.int/documents/247904/349490/S2_MSI_Product_Specification.pdf)

## Data browser
Data can be browsed here: http://sentinel-s2-l1c.s3-website.eu-central-1.amazonaws.com/


# Running

Prerequisites:
1. SciHub account
2. Access to Amazon S3 bucket
3. Enough local storage space to download and unpack SciHub products

	java -Dconfig=ingestor.properties com.sinergise.sentinel.ProductsIngestorRunner


# Relevant links
Mailing list for discussion is at http://lists.osgeo.org/mailman/listinfo/sentinel-pds
