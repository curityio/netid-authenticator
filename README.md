# NetID Access Authenticator Plugin

[![Quality](https://img.shields.io/badge/quality-production-green)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-binary-blue)](https://curity.io/resources/code-examples/status/)

A NetID Access Authenticator Plugin for the Curity Identity Server.

**Note**: The plugin requires at least version 7.0. of the Curity Identity Server. It might not work with previous versions.

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

## Migrating from internal plugin

If you are already running the internal Curity authenticator that was available in versions before 7.0, the configuration will need tbe changed slightly
### Plugin type
The type of the plugin changed from `netidaccess` to `netidaccess-os`. This affects an xml-tag and a namespace. 
Internal plugin config:
```
<authenticator>
    <id>NetIdAccess</id>
    <netidaccess xmlns="https://curity.se/ns/conf/authenticator/netid-access">
    …
    </netidaccess>
</authenticator>
```

Migrated config:
```
<authenticator>
    <id>NetIdAccess</id>
    <netidaccess-os xmlns="https://curity.se/ns/ext-conf/netidaccess-os">
    …
    </netidaccess>
</authenticator>
```

### HTTP Client
The reference to a http-client now needs an `<id>` element.
```
<http-client>trustStoreHttpClient</http-client>
```

Migrated config:
```
<http-client>
    <id>trustStoreHttpClient</id>
</http-client>
```

## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
