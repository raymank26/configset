![CI](https://github.com/configset/configset/workflows/CI/badge.svg)

# Overview

![diagram](https://i.imgur.com/OEo5xik.png)

The project aims to bring the feature of runtime configuration to the masses. The configuration is list of options
helping you to control the behaviour of your applications. More precisely *configset* provides following features:

1. Manages the configuration in the single dedicated place.
2. Distributes configuration across different physical servers/VMs/containers using language-specific SDK. Holds configuration state in memory.
3. Allows people from of a team (DevOps, managers, developers) to observe and manage the configuration using a web dashboard.
4. Tracks changes of the configuration and provides "listening" API to call application code to reflect the change (
update internal state, call a function, etc.).

A structure of the configuration is straightforward: the configuration is split by **applications**. Each **application** 
has many **properties**. Each **property** is a list of (**hostname**, **key**, **value**) triples. Each client has *hostname*
parameter. When a client asks for a particular
**property** in a **config application** then only **value** for a given **hostname** is returned (or default one if configured).

There is no difference to the actual purpose of configuration: infrastructure, application features, secrets, etc.

# Architecture

There are four parts: a client library, a server application and a dashboard.

1. Client connects to gRPC server application, receives a current configuration snapshot, listens for future configuration changes.
2. Server stores configuration in some sort of storage (by now two options exist: *in memory* for tests or external PostgreSQL) and
handles outgoing streams to connected clients. At every point of time it knows which client is looking for which configuration.
3. CRUD-like UI to apply changes to the system.

# Usage example

[Sample project](https://github.com/configset/configset/tree/master/sample)

To run the sample (docker-compose is required) execute locally ```./sample_run.sh```. Dashboard is available at ```http://localhost:8080/```

# Development

1. ```./local_run.sh``` - starts db/dashboard/server in docker-compose locally. A dashboard will be available at
```http://localhost:8080/```.
2. ```./gradlew test``` - runs tests across the project.

# Deployment

1. Build images - `./gradlew server:dockerBuildImage && ./dashboard:dockerBuildImage`. And push them into the registry.
2. Deploy (or use existed) one of the available storage backend. Buy now Postgresql is the only option. PRs are welcome!
3. Deploy ```server``` and ```dashboard``` to your infrastructure using [compose file](https://github.com/configset/configset/blob/master/docker-compose.yml)
as an example.
4. Publish Java SDK to your maven repository, add artifact to your project.

# License

Apache-2.0