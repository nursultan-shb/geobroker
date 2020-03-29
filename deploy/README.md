# Deployment of DynamicBalancer

This document provides a set of instructions to launch DynamicBalancer on Amazon Web Services (AWS). To deploy the setup and start services, we use [Ansible](https://www.ansible.com/) that is an open-source application deployment and software provisioning tool. 
The current directory contains Ansible scripts and *.jar package files of DynamicBalancer services and structured as follows: 
1. The */jar* directory includes package and configuration files of the services: Clients, LoadBalancerService, PlanCreatorService, and GeoBroker.
2. The */tasks* directory contains a subset of Ansible scripts.
3. The *ansible.cfg* file is used for authentication purposes. It contains a key pair name that is used to access AWS EC2 instances. 
4. The *variables.yml* file contains different configuration parameters of the environment. 
For example, it is possible to change instance types of servers, their AMI, or a cluster size. More details are available in the file.
Additionally, this file should include AWS authentication parameters: AWS_DEFAULT_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY. 
They will be added as environment variables in GeoBroker EC2 instances in order to get CPU load via AWS GetMetricStatisticsRequest API.
5. The *main.yml* file contains a set of commands to create the server infrastructure on AWS and deploy DynamicBalancer services.
It launches the following AWS services:
    - AWS VPC for DynamicBalancer.
    - AWS VPC subnets.
    - The Internet Gateway.
    - A security group.
    - EC2 instances for DynamicBalancer components.
    
Once the infrastructure is ready and all files are copied, the command launches the DynamicBalancer services. 

6. The *clients_start.yml* file contains a set of commands to create the infrastructure on AWS for Clients and deploy client services.
7. The *terminate_instances.yml* file contains a set of commands to terminate all EC2 instances. 
It can be used after each experiment.
8. The *terminate_environment.yml* file contains a set of commands to terminate the rest of AWS services. 
It can be used as a final clearance step when all experiments are done. Note, this command should be run after the playbook *terminate_instances.yml*.

## Before running DynamicBalancer services
Before running Ansible scripts, one needs to install Ansible on a local machine and configure AWS credentials. 
Below, there is an example of fulfilling these procedures on AWS EC2 machine. Note, in this example, we store AWS credentials in the environment variables. One can use other options to specify AWS credentials, e.g., in a configuration file.
- [Install python3 and boto3](https://aws.amazon.com/premiumsupport/knowledge-center/ec2-linux-python3-boto3).
- Install ansible: `pip3 install ansible`.
- As some of Ansible commands require *boto*, install: `pip install boto`. 
- Install AWS CLI: `pip3 install awscli`.
- Configure AWS credentials using environment variables:\
  `export AWS_DEFAULT_REGION='YOUR_REGION'`\
  `export AWS_ACCESS_KEY_ID='YOUR_ACCESS_KEY_ID'`\
  `export AWS_SECRET_ACCESS_KEY='YOUR_SECRET_ACCESS_KEY'` 
- In *variables.yml*, replace AMI ID of EC2 instance with AMI ID that is available to your account.
- In *variables.yml*, add AWS credentials to *os_environment* properties.  
- Add a public key *.pem file for accessing EC2 instances to the current folder. Set the file permission: `chmod 600 <filename>`.\
Put the name of the key in the property *private_key_file* of the *ansible.cfg* file 
and in the property *keypair* of *variables.yml*.

## Run DynamicBalancer services
Configure the setup in *variables.yml* if needed.\
To create the server infrastructure and start DynamicBalancer services, run `ansible-playbook main.yml`. \
When the command is successfully finished, DynamicBalancer is ready to accept client messages. 
The address of the load balancer where clients can send their messages to is available in properties *loadbalancer_ip* and *loadbalancer_frontend_port* of the *variables.yml* file.

## Run clients
This section describes the current way of running clients. 
Note, you can build your own clients and direct messages to the loadbalancer of DynamicBalancer as long as they send message types supported by [GeoBroker](https://github.com/MoeweX/geobroker).
In our case, we use [HikingGenerator](https://github.com/MoeweX/IoTDSG/blob/master/IoTDSG/src/main/kotlin/de/hasenburg/iotdsg/HikingGenerator.kt) that generates files to simulate a hiking scenario where clients travel on predefined routes.\

The generated files are to be grouped and compressed into directories. Each group of files will be executed on a separate EC2 instance.
A name of each directory should start with an increasing integer number. For example, the first EC2 client instance will take care of a directory named '0.zip', the second - for a directory '1.zip'.
Put compressed files into the directory: *deploy\jars\client\zipped*. 
The playbook `clients_start.yml` will start exactly the same number of EC2 client instances as a number of compressed directories. 
The current repository contains three compressed directories, i.e, three EC2 client instances will be started. 

To start clients, run: `ansible-playbook clients_start.yml`. When an experiment ends, this playbook fetches results (*wasSent.txt and *wasReceived.txt files) into the folder: *deploy\jars\client\results*.

## Terminate the environment
To terminate all EC2 instances, run: `ansible-playbook terminate_instances.yml`.\
To terminate all other AWS services, run: `ansible-playbook terminate_environment.yml`.





 