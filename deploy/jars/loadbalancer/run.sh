#!/bin/bash
nohup java -jar -Xms1000m -Xmx6000m  loadbalancer-server.jar configuration.toml&
echo 0
