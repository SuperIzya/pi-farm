

## Preparing raspberry

## Concepts

### Platform

**Raspberry farm** is a platform in a sense that the most essential parts 
of the system (Drivers/Configuration nodes/etc.) are search through JAR-files
in CLASS_PATH. Each plugin that wants to provide such parts should provide
object extending [PiManifest](https://github.com/SuperIzya/pi-farm/blob/master/common/src/main/scala/com/ilyak/pifarm/PiManifest.scala).
 

### Driver

Every micro-controller has it's own attached accessories and it has 
it's own specifics of how to upload new sketch on it. Every micro-controller
has once connected to the server (for now it's only USB) has it's own
unique endpoint. Once driver has been assigned to the endpoint, this data
is saved to the database (by default embedded H2), so that after restart, 
correct driver will start for this device.

It is also driver's duty to provide [akka.stream.Graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html)
connector for the device. Since the device may have some sensors as well as
servos, valves or any other controllable digital device, connector should
provide outputs for the sensors data and inputs for the commands for the
devices.


### Connection

Although **connection** is not plugable *per ce*, but it is never the less
one of the cornerstones of the system. It allows to construct  [akka.stream.Graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html)
at run-time while providing 'physical sense-safety'. **Connection** will not
allow to connect humidity sensor to input expecting temperature.
    

### Configuration

**Configuration** is a [directed graph](https://en.wikipedia.org/wiki/Directed_graph) 
defining data flow that starts from sensors (connected to micro-controller) 
and ends at the digital devices (servo/valve/etc.) also connected to micro-controller.

Each directed edge is a **connection** (as described above). This also includes
external connections, the ones that should be connected to micro-controllers. 
Since **connections** 'physical sense-safe', only particular combinations of 
**configuration(s)** and **driver(s)** are valid. Valid combinations 
are stored in DB, to support persistency. 

Although it is transformed to [akka.stream.Graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html), 
**configuration** is dynamic in nature. The idea is to be able to change
the configuration during the runtime without the need to recompile the 
whole system, nor any part of it.

The **configuration** graph is written in JSON-like structure. Each node 
of this structure contains:
* id unique throughout graph
* names of the inputs and outputs of this node
* name of the plugin and implementation of [ConfigurableNode](https://github.com/SuperIzya/pi-farm/blob/master/common/src/main/scala/com/ilyak/pifarm/flow/configuration/ConfigurableNode.scala),
 that will uniquely define the class to backup the node
* in case node is a container, e.g. it contains internal graph, the graph, 
containing the node, also will contain inner graph with the same id as the node.
This inner graph should have the same inputs and outputs as the node containing it.
Otherwise the graph will be invalid.

As being mentioned earlier, given some **configuration**, **Raspberry farm** system will build [akka.stream.Graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html).
In order to run this [akka.stream.Graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html), 
all it's external connections should be connected to appropriate sources and sinks (inputs and outputs of the **driver(s)**).
If any of the external connections (as well as internal for that matter) 
are not properly connected (connected to incorrect type or not connected at all)
the configuration would not run and attempt to run such malformed configuration
will result with error.

