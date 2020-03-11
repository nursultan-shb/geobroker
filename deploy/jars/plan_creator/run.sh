#!/bin/bash
nohup java -jar -Xms1000m -Xmx4000m  plan-creator-server.jar configuration.toml&
echo 0
