# Deployment of DynamicBalancer

This folder contains Ansible scripts to deploy the environment on Amazon Web Services (AWS). Before launching the scripts, one needs to make sure that:
1. */jar* folder includes *.jar files of the services: Clients, LoadBalancerService, PlanCreatorService, and GeoBroker.
2. For authentication, we use *ansible.cfg* file. It contains the name of a public key. 
3. */key* folder should include a public key file *.pem.

To create the DynamicBalancer environment, one needs to run the Ansible command: `ansible main.yml`. The following AWS services are launched:
1. AWS VPC for DynamicBalancer.
2. AWS VPC subnets.
3. Internet Gateway.
4. Security group.
5. EC2 instances for DynamicBalancer components.

Once the infrastructure is ready and all files are copied, the command launches the DynamicBalancer services. 

To start clients, one needs to run: `ansible clients_start.yml`. <br />
To terminate all EC2 instances, one needs to run: `ansible terminate_instances.yml`. <br />
To terminate DynamicBalancer environment, one needs to run: `ansible terminate_environment.yml`. <br />

The file *variables.yml* contains different configuration parameters for launching the environment. It should include AWS authentication parameters as: AWS_DEFAULT_REGION, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY. They will be added as environment variables in GeoBroker EC2 instances to use AWS GetMetricStatisticsRequest API (to get CPU load).

 