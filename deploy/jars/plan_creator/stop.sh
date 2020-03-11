#!/bin/bash
ps -ef | grep "plan-creator-server.jar" | awk '{print $2}' | xargs kill

