#!/bin/bash
ps -ef | grep "GeoBroker-Server.jar" | awk '{print $2}' | xargs kill

