# GeoBroker

[![CodeFactor](https://www.codefactor.io/repository/github/moewex/geobroker/badge)](https://www.codefactor.io/repository/github/moewex/geobroker)

GeoBroker is a pub/sub system research prototype that not only uses content information, i.e., the topic of a message, 
but also the geo-context of publishers and subscribers for the matching of messages.

As this project contains multiple git submodules, one needs to run the following after cloning:
```
git submodule init
git submodule update
```

To start the Server, run the main method of *de.hasenburg.geobroker.server.main.Server.java*. 
A very simple client can be started by running *de.hasenburg.geobroker.client.main.SimpleClient.java*.

## Usage of Code in Experiments

The code found in this repository has been used for some expriments:
- The original code for GEO experiments (geo-context information is used) is accessible in the GEO branch or release 
[v1.0](https://github.com/MoeweX/geobroker/releases/tag/v1.0).
- The original code for NoGEO experiments (geo-context information is not used) is accessible in the NoGEO branch or 
release [v1.0-noContext](https://github.com/MoeweX/geobroker/releases/tag/v1.0-noContext).

## What is this Fork?
This Fork contains the source code for my Master's thesis 'Scaling GeoBroker Clusters Based on Local Sharding'. Currently, it is in an active development stage.