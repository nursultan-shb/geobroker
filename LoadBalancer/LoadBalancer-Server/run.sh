#!/bin/bash
rm nohup.out
nohup java -jar -Xms1000m -Xmx8000m  loadbalancer-server.jar&
