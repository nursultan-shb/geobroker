# DynamicBalancer

DynamicBalancer is a horizontal scalable pub/sub research prototype that distributes client messages across GeoBroker clusters.
The load balancing mechanism considers live characteristics of passing traffic to dynamically adjust the load on servers. 
DynamicBalancer is location-aware, as it additionally considers the geo-context of data to reduce excessive data dissemination and enable clients to have accurate control of message delivery.

## GeoBroker

[![CodeFactor](https://www.codefactor.io/repository/github/moewex/geobroker/badge)](https://www.codefactor.io/repository/github/moewex/geobroker)

GeoBroker is a pub/sub system research prototype that not only uses content information, i.e., the topic of a message, 
but also the geo-context of publishers and subscribers for the matching of messages.
The original code of GeoBroker is accessible in: https://github.com/MoeweX/geobroker.

## Deployment

To deploy DynamicBalancer, follow: `deploy/READ.ME`