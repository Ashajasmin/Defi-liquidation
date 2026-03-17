# docker-actus-rf20 
## Overview
This repository provides materials to configure and start a network of docker containers which can simulate the cash flows of financial contracts defined with the ACTUS standard using different risk scenarios, i.e. different risk modelling environments. The contract type sensitive cashflow simulation logic executes in an actus-server-rf20 docker container; the risk scenario management and handling executes in a separate actus-riskserver-ce docker container. 

The repository contains docker compose files defining configurations for:
* the ACTUS Quickstart network configuration - which includes containers for: actus-server-rf20, actus-riskserver-ce, and mongodb
* an ACTUS Demonstration configuration - which includes an additional container for an actus-rshiny-demo

## Recommended QUICKSTART ACTUS SERVICE configuration 
* For new ACTUS users wanting to deploy and ACTUS contract simulation service quickly and start experimenting with it, the ACTUS quickstart configuration is recommended. 
* The ACTUS demonstration configuration adds an interactive browser based demo showing simulation of contract and sample portfolio, for selected risk environments , together with plots of the generated cashflows. It also includes the capability to import and export sample portfolio from spreadsheets on your workstation. 

## QUICKSTART Deployment of an initial ACTUS SERVICE using Docker Compose - Steps
### Prerequisites
*   You must have docker (a docker daemon) installed in your environment. Installing Docker Desktop is a convenient way to do this. Documentation on how to install Docker Desktop can be found under: https://docs.docker.com/desktop/ 
*   Ports 8082 8083 3000 and 27018 in your work environment must be available ( i.e. not in use by some other application )
    *  If this quickstart ACTUS deployment is being run a second time - you may need to stop and exit previous processes using these ports      

### Installation
1.  Navigate to a folder where you want to install your ACTUS service - we will refer to this folder as ACTUS_BASE
2.  Clone this git repository to a local copy in your ACTUS_BASE folder using the command: > git clone https://github.com/fnparr/docker-actus-rf20.git
3.  Navigate to the ACTUS_BASE/docker-actus-rf20 folder
4.  Issue the command: > docker compose -f quickstart-docker-actus-rf20.yml -p quickstart-docker-actus-rf20 up

The terminal where you issued this command will start displaying console scripts of the started containers and network. 

At this point if you have docker desktop installed - you should be able to see in its dashboard:
*    In the images panel, locally copied and saved images for:
     * actus-server-rf20:latest  ( it may also be tagged: v1.1.0.build1) 
     *   actus-riskserver-ce:latest ( it may also be tagged: v1.1.0.build1 ) 
     *   mongodb
*    in the containers panel
     *   a docker-compose network named quickstart-docker-actus-rf20
     *   if you click to expand this - running containers: 
         *    actus-server-rf20:nodb
         *    actus-riskserver-ce:mdb27018
         *    mongodb

### Experiment with curl commands requesting ACTUS services from the command line 
The following steps can be used to validate that you have a working QuickStart ACTUS installation. 
If you want to build an application using an ACTUS service to simulate future cashflows of an ACTUS contract defined by you,
using interest rate and market risk scnarios defined by you, the flow through your application is likely to match some part  or subset  
of the of the sequence of validation tests described below. 

1.  Start a new terminal window and navigate to ACTUS_BASE/docker-actus-rf20/actus-riskserver-ce/actus-riskservice/testB
2.  Run the (linux/MacOS) command:   > source putUst5Y_falling.txt
    * this will the actus-riskserver-ce risk data api to insert a new reference index into the persistent mongodb risk store
    * the response from the command should end with the line > Reference Index successfuly created
3.  Run the command:   > curl -v http://localhost:8082/findAllReferenceIndexes
    * this will read back the newly created reference index data from the mongo risk Store and display it
4.  Run the command:   > source l3PAMwRF.txt
    * this will return and display the cashflows generate by the actus service for simulating a contract
    * early in the displayed cashflow the name value pair:  > "status":"Success"
      indicates that contract simulation was successful.
      
If all of the above tests run as expected, you have a working actus-riskserver-ce container, properly configured to use 
persistent storage in a mongodb container and an actus-riskserver. capable of simulating contract. At this point the 
more complete sequence of tests in ACTUS_BASE/docker-actus-rf20/actus-riskserver-ce/actus-riskservice/testB/TestB_script.txt can be run.

### View, test,  or stop a docker compose network - for any of the above examples
Use Docker Desktop Dashboard to view the docker compose network you have started.

The sequence of commands in docker-actus-rf20/actus-riskserver-ce/actus-riskservice/tests/TestB_script.txt
can be run from a terminal to validate that the installation is working 

To stop the network:
*   use the stop button in Docker Desktop Dashboard. or
*   CNTROL C in the terminal window where you issued > docker compose YYY up, or
*   from any command line: > docker compose XXXX down

where YYY is the name of the docker compose configuration and XXX is the name of the docker compose network which you started. 

## An Alternate QUICK ACTUS DEMO configuration - exploring use of ACTUS with a reactive R-Shiny demo 
For new ACTUS users wanting to start experimenting with using ACTUS for contract cashflow simulations quickly, it is recommended for an initial network to include:
*  actus-server-rf20 docker container - with a contract simulation API and contract type specific logic to simulate future cashflows of any contract
*  actus-riskserver-ce docker container - providing an external community-edition riskservice with a risk entity store api and the risk factor 2.0 interface to provide the actus-server-rf20 risk model observation results
*  a mongodb docker container providing persistent storage for risk entities created using the risk entity store api to the actus-riskserver-ce
*  an actus-rshiny-demo docker container providing a reactive demo showing how an actus service can be used to simulate futurecashflows of ACTUS contracts and portfolios under different interest rate risk scenarios 

### Prerequisites
*   You must have docker (a docker daemon) installed in your environment. Installing Docker Desktop is a convenient way to do this. Documentation on how to install Docker Desktop can be found under: https://docs.docker.com/desktop/ 
*   Ports 8082 8083 3000 and 27018 in your work environment must be available ( i.e. not in use by some other application )
    *  If this quickstart ACTUS deployment is being run a second time - you may need to stop and exit previous processes using these ports      

### Installation
1.  Navigate to a folder where you want to install your ACTUS service - we will refer to this folder as ACTUS_BASE
2.  Clone this git repository to a local copy in your ACTUS_BASE folder using the command: > git clone https://github.com/fnparr/docker-actus-rf20.git
3.  Navigate to the ACTUS_BASE/docker-actus-rf20 folder
4.  Issue the command: > docker compose -f rshinydemo-docker-actus-rf20.yml -p rshinydemo-docker-actus-rf20 up

The teminal where you issued this command will start displaying console scripts of the started containers and network. 

At this point if you have docker desktop installed - you should be able to see in its dashboard:
*    In the images panel, locally copied and saved images for:
     * actus-server-rf20:latest
     *   actus-riskserver-ce:latest
     *   mongodb
*    in the containers panel
     *   a docker-compose network named quickstart-docker-actus-rf20
     *   if you click to expand this - running containers: 
         *    actus-server-rf20:latest ( or v1.1.0.build1)
         *    actus-riskserver-ce:latest ( or v1.1.0.build1)  
         *    actus-rshiny-demo:b03
         *    mongodb

### Experiment by using the actus-rshiny-demo to see how ACTUS can simulate contract cashflows 
After bringing up the the quickstart network you can use the actus Rshiny demonstration to see how an ACTUS service can simulate the future cashflows of a single ACTUS contract or a portfolio of ACTUS contracts under different interest rate risk scenarios. 

Successful operation of the reactive RShiny demonstration will also validate that your quickstart-docker-actus-rf20 configuration
in installed and running as expected. 

1. Point a browser at localhost:3000
2. Go to the help tab and set the target actus-server t0 be:  host.docker.internal:8083/  ( you need the / at the end and http NOT https ). 
3. Click on the loan contract cashflow tab

The demonstration is reactive (point and click) and to a degree self documenting. A deeper explanation of the concepts behind this demo is 
available at https://documentation.actusfrf.org/docs/dadfir3-demo/Demo%20User%20Guide.

## Downloading required images - view dockerfiles
A Prerequisite ( for ALL steps in using docker-actus-rf20) is that you must have docker (a docker daemon) installed in your environment. 
Installing Docker Desktop is a convenient way to do this.

The images for actus-server-rf20 and actus-riskserver-ce are pulled from publicly accessible dockerhub/actusfrf repositories 

The source code and dockerfiles used to build these images can be viewed at:
* https://github.com/actusfrf/actus-service.git
* https://github.com/actusfrf/actus-riskservice.git 

The build process for each of these images imports the actus-core.jar library. 
You can request access to view soure code of this core ACTUS reference implementation library using the form at: https://www.actusfrf.org/developers 
