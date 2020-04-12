# DynamicBalancer

With the rapid growth of mobile connected devices, Internet of Things (IoT) applications face a great scalability challenge.
IoT services often use the publish-subscribe (pub/sub) communication paradigm for the effective distribution of messages. 
We propose **DynamicBalancer**, a horizontally scalable topic-based pub/sub system. 
Our solution is location-aware, as it additionally considers the geo-context of data to reduce excessive data dissemination and enable clients to have  accurate control of message delivery. 
DynamicBalancer distributes client messages across **GeoBroker** nodes. The load balancing mechanism considers live characteristics of passing
traffic to dynamically adjust the load on servers. 

## GeoBroker

[![CodeFactor](https://www.codefactor.io/repository/github/moewex/geobroker/badge)](https://www.codefactor.io/repository/github/moewex/geobroker)

GeoBroker is a pub/sub system research prototype that not only uses content information, i.e., the topic of a message, 
but also the geo-context of publishers and subscribers for the matching of messages.
The original code of GeoBroker is accessible in: https://github.com/MoeweX/geobroker.

As this project contains multiple git submodules, one needs to run the following after cloning:
```
git submodule init
git submodule update
```

## Deployment

To deploy DynamicBalancer, follow: `deploy/READ.ME`