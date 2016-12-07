#!/bin/sh
curl -v -X PUT http://$1/appscale-benchmarks
curl -v -X PUT http://$1/appscale-logs
curl -v -X PUT http://$1/appscale-apicalls

curl -v -X PUT -d @es_template.json http://$1/_template/appscale-benchmarks
curl -v -X PUT -d @es_template.json http://$1/_template/appscale-logs
curl -v -X PUT -d @es_template.json http://$1/_template/appscale-apicalls