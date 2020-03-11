#!/bin/bash
nohup java -jar -Xms1000m -Xmx4000m  GeoBroker-Server.jar configuration.toml&
echo 0
