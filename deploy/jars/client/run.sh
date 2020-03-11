#!/bin/bash
nohup java -jar  -Xms200m -Xmx6000m  MultiFileClient.jar --dir "files" --ip-address "18.184.236.160" --port 7225&
echo 0
