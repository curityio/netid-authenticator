# NetID Access Authenticator Plugin

[![Quality](https://img.shields.io/badge/quality-production-green)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-binary-blue)](https://curity.io/resources/code-examples/status/)

A NetID Access Authenticator Plugin for the Curity Identity Server.

## Building the Plugin

Build the plugin by issuing the command `mvn package`. This will produce a JAR file in the `target` directory, which can be installed.

## Installing the Plugin

### Installing from Release Package

To install the plugin, download the release zip archive, unpack it and copy the contents of the `/usr/` to `${IDSVR_HOME}/usr/`, 
on each node of the Curity Identity Server, including the admin node.

### Installing from Source

To install the plugin after building, copy the contents of `/target/usr/` to `${IDSVR_HOME}/usr/`, on each node of
the Curity Identity Server, including the admin node.

For more information about installing plugins, refer to the [curity.io/plugins](https://support.curity.io/docs/latest/developer-guide/plugins/index.html#plugin-installation).

## Required Dependencies

For a list of the dependencies and their versions, run `mvn dependency:list`. Ensure that all of these are installed in the plugin group; otherwise, they will not be accessible to this plug-in and run-time errors will result.

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
