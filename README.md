![CI](https://github.com/letsconfig/letsconfig/workflows/CI/badge.svg)

# Overview

The project aims to bring the feature of runtime configuration to masses. 

Basically the approach is here: there are many **config applications**. Every **config application** 
has many **properties**. Each **property** has a list of (**hostname**, **value**) pairs and a default value. When a client asks for a particular
**property** in a **config application** then only **values** for a given **hostname** are returned and then listened for updates.

It's especially useful for complex applications and massive deployments where redeployment is costly and not desirable.

There is no difference to the actual purpose of configuration: infrastructure, application features, secrets, etc.

# Architecture

There are four parts: a client library, a server application and an CRUD-like UI.

1. Client connects to gRPC server application to get current configuration snapshot and listens for future configuration changes.
2. Server manages configuration to some sort of storage (by now two options exist: *in memory* for tests or external PostgreSQL) and
handles outgoing streams to connected clients. At every point of time it knows which client is looking for which configuration.
3. CRUD-like UI to apply changes to the system.

# Code sample

TODO() // see tests

