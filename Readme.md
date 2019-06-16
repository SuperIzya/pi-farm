# Raspberry farm for microcontrollers

Raspberry farm is a platform for controlling servos/valves/etc, 
that are connected to micro-controllers, based on data from sensors from
those or other micro-controllers.

In other words: if have a lot of different IoT devices, that have sensors 
and/or some servos/valves/etc attached, and you want a cheap way to 
controll all those servos/valves/etc based on data received from sensors, 
**Raspbery farm** is your guy.
 
## Why Raspberry farm

* It is JVM-based hence whole JVM ecosystem is available.
* It uses as few resources as possible due to underlying 
[akka.stream.Graph](https://doc.akka.io/docs/akka/current/stream/stream-graphs.html).
* It allows to define new types of rules/micro-controllers/etc in a 
developer friendly way.
* (Soon) It allows to coordinate several micro-controllers.
* (Soon) It allows in a user friendly way to manage controlling rules and apply 
the changes on the fly.

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
allow to connect humidity sensor to input awaiting temperature.
    


## Preparing raspberry

1. Setup ssh access (instructions are [here](https://www.raspberrypi.org/documentation/remote-access/ssh/))
1. Download and extract arm version of Arduino IDE (from [here](https://downloads.arduino.cc/arduino-1.8.8-linuxarm.tar.xz))
1. Run `sudo ln -s [Extracted folder]/arduino /usr/bin/arduino` 
1. Run `arduino --version`. Should be not less than 1.8.8 
