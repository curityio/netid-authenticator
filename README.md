# NetID Access Authenticator Plugin

[![Quality](https://img.shields.io/badge/quality-production-green)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-binary-blue)](https://curity.io/resources/code-examples/status/)

A NetID Access Authenticator Plugin for the Curity Identity Server.

## Building the Plugin


Build the plugin by issuing the command `mvn package`. This will produce a JAR file in the `target` directory, which can be installed.

## Installing the Plugin

To install the plugin, copy the following files on each node, including the admin node:

- the compiled JAR and all of its dependencies into the `${IDSVR_HOME}/usr/share/plugins/${pluginGroup}`,
- the contents of the `resources/templates` directory to `${IDSVR_HOME}/usr/share/templates/overrides`,
- the contents of the `resources/messages` directory to `${IDSVR_HOME}/usr/share/messages/overrides/en`,
- the `images/login-symbol-netid.svg` file to `${IDSVR_HOME}/usr/share/webroot/assets/images`.

For more information about installing plugins, refer to the [curity.io/plugins](https://support.curity.io/docs/latest/developer-guide/plugins/index.html#plugin-installation).

## Required Dependencies

For a list of the dependencies and their versions, run `mvn dependency:list`. Ensure that all of these are installed in the plugin group; otherwise, they will not be accessible to this plug-in and run-time errors will result.

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
