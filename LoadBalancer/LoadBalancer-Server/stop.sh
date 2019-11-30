#!/bin/bash
ps -ef | grep "loadbalancer-server.jar" | awk '{print $2}' | xargs kill

