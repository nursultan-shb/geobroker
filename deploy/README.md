# Deployment of DynamicBalancer

This document provides a set of instructions to launch DynamicBalancer on Amazon Web Services (AWS). To deploy the setup and start services, we use Ansible that is an open-source application deployment and software provisioning tool. 
The current directory contains Ansible scripts and *.jar package files of DynamicBalancer services and structured as follows: 
1. The */jar* directory includes package and configuration files of the services: Clients, LoadBalancerService, PlanCreatorService, and GeoBroker.
2. The */tasks* directory contains a subset of Ansible scripts.
3. The *ansible.cfg* file is used for authentication purposes. It contains the name of a public key. 
4. The *variables.yml* file contains different configuration parameters for launching the environment. For example, it is possible to change instance types of servers, their AMI, or a cluster size. More details are available in the file.
Additionally, this file should include AWS authentication parameters: AWS_DEFAULT_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY. They will be added as environment variables in GeoBroker EC2 instances to use AWS GetMetricStatisticsRequest API (to get CPU load).
5. The *main.yml* file contains a set of commands to create the server infrastructure on AWS and deploy DynamicBalancer services.
The following AWS services are launched:
    - AWS VPC for DynamicBalancer.
    - AWS VPC subnets.
    - The Internet Gateway.
    - A security group.
    - EC2 instances for DynamicBalancer components.
Once the infrastructure is ready and all files are copied, the command launches the DynamicBalancer services. 

6. The *clients_start.yml* file contains a set of commands to create the infrastructure on AWS for Clients and deploy the client service there.
7. The *terminate_instances.yml* file contains a set of commands to terminate all EC2 instances. It is used at the end of each experiment.
8. The *terminate_environment.yml* file contains a set of commands to terminate AWS environment.

##Launch the server environment
Before creating the infrastructure, one needs to add a public key file *.pem to the current folder and put the name of the key in the property *private_key_file* of the *ansible.cfg* file.\
To create the server infrastructure and start DynamicBalancer services, run `ansible-playbook main.yml`. When the command is successfully finished, DynamicBalancer is ready to accept client messages. 
The address of the load balancer where clients can send their messages to is available in properties *loadbalancer_ip* and *loadbalancer_frontend_port* of the *variables.yml* file.

##Launch clients
To start clients, one needs to run: `ansible-playbook clients_start.yml`.

##Terminate the environment
To terminate all EC2 instances, one needs to run: `ansible-playbook terminate_instances.yml`. <br />
To terminate DynamicBalancer environment, one needs to run: `ansible-playbook terminate_environment.yml`. <br />



 