#!/bin/sh

sleep 120

while true
do
	/usr/bin/java -jar /home/sibbo/.websitechangetracker/WebsiteSurveillance.jar
	sleep 600
done
