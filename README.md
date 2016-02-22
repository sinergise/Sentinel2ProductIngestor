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

## Tile locations
Each file is its own object in Amazon S3. The data are organised per tiles using the Military grid system. The latest KML file describing available tiles is available is ESA site (https://sentinels.copernicus.eu/web/sentinel/missions/sentinel-2/data-products), or via direct link (https://sentinels.copernicus.eu/documents/247904/1955685/S2A_OPER_GIP_TILPAR_MPC__20151209T095117_V20150622T000000_21000101T000000_B00.kml/ec05e22c-a2bc-4a13-9e84-02d5257b09a8). SHP version is available here: https://github.com/justinelliotmeyers/Sentinel-2-Shapefile-Index


# Running

Prerequisites:
1. SciHub account
2. Access to Amazon S3 bucket
3. Enough local storage space to download and unpack SciHub products

	java -Dconfig=ingestor.properties com.sinergise.sentinel.ProductsIngestorRunner


# Notification service for new products
There is a public SNS topic that anyone can subscribe to for notifications of when a new Sentinel-2 product has been added to s3://sentinel-pds. The topic publishes a message when a new product is fully ingested to S3.

	ARN: arn:aws:sns:eu-west-1:214830741341:NewSentinel2Product
Find example of the message here:
	https://github.com/sinergise/Sentinel2ProductIngestor/wiki/SNS-Topic-for-new-products


# Relevant links
Mailing list for discussion is at http://lists.osgeo.org/mailman/listinfo/sentinel-pds
