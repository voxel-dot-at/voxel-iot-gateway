# CHANGELOG

## Upgrade to Leshan 2.0.0-SNAPSHOT

### Breaking Changes

- Upgraded from Leshan 1.x to Leshan 2.0.0-SNAPSHOT
- Java servlet API changed from `javax.servlet` to `jakarta.servlet`
- Jetty EventSource API moved to `org.eclipse.jetty.ee10.servlets`
- Leshan security classes moved to `org.eclipse.leshan.servers.security`
- Demo CLI classes moved to `org.eclipse.leshan.demo.cli`
- Demo server CLI classes moved to `org.eclipse.leshan.demo.servers.shared.cli`

### Updated Dependencies

- Updated Maven dependencies to use new Leshan module names:
  - `leshan-server-cf` → `leshan-tl-cf-server-coap` + `leshan-tl-cf-server-coap-oscore`\n  - `leshan-core-demo` → `leshan-demo-shared`\n  - `leshan-server-core-demo` → `leshan-demo-servers-shared`\n  - `leshan-server-redis` → `leshan-lwm2m-server-redis`\n  - `leshan-tl-javacoap-server` → `leshan-tl-jc-server-coap` + `leshan-tl-jc-server-coaptcp`\n\n### Code Changes

#### IoTGateway.java
- Updated imports for security classes:\n  - `org.eclipse.leshan.server.security.EditableSecurityStore` → `org.eclipse.leshan.servers.security.EditableSecurityStore`\n  - `org.eclipse.leshan.server.security.FileSecurityStore` → `org.eclipse.leshan.servers.security.FileSecurityStore`\n- Updated SecurityServlet import:\n  - `org.eclipse.leshan.server.core.demo.json.servlet.SecurityServlet` → `org.eclipse.leshan.server.servlet.SecurityServlet`\n\n#### EventServlet.java\n- Updated servlet imports:\n  - `javax.servlet.http.HttpServletRequest` → `jakarta.servlet.http.HttpServletRequest`\n  - `org.eclipse.jetty.servlets.EventSource` → `org.eclipse.jetty.ee10.servlets.EventSource`\n  - `org.eclipse.jetty.servlets.EventSourceServlet` → `org.eclipse.jetty.ee10.servlets.EventSourceServlet`\n- Updated Californium endpoint import:\n  - `org.eclipse.leshan.server.californium.endpoint.CaliforniumServerEndpoint` → `org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpoint`\n\n#### IoTGatewayCli.java\n- Updated CLI imports:\n  - `org.eclipse.leshan.core.demo.cli.StandardHelpOptions` → `org.eclipse.leshan.demo.cli.StandardHelpOptions`\n  - `org.eclipse.leshan.core.demo.cli.VersionProvider` → `org.eclipse.leshan.demo.cli.VersionProvider`\n  - `org.eclipse.leshan.core.demo.cli.converters.PortConverter` → `org.eclipse.leshan.demo.cli.converters.PortConverter`\n  - `org.eclipse.leshan.server.core.demo.cli.DtlsSection` → `org.eclipse.leshan.demo.servers.shared.cli.DtlsSection`\n  - `org.eclipse.leshan.server.core.demo.cli.GeneralSection` → `org.eclipse.leshan.demo.servers.shared.cli.GeneralSection`\n  - `org.eclipse.leshan.server.core.demo.cli.IdentitySection` → `org.eclipse.leshan.demo.servers.shared.cli.IdentitySection`\n\n#### JacksonSecurityDeserializer.java\n- Updated SecurityInfo import:\n  - `org.eclipse.leshan.server.security.SecurityInfo` → `org.eclipse.leshan.servers.security.SecurityInfo`\n- Removed unused SecurityUtil import\n\n#### ServerServlet.java\n- Updated servlet imports:\n  - `javax.servlet.*` → `jakarta.servlet.*`\n- Updated JSON serialization imports:\n  - `org.eclipse.leshan.server.core.demo.json.PublicKeySerDes` → `org.eclipse.leshan.demo.servers.json.PublicKeySerDes`\n  - `org.eclipse.leshan.server.core.demo.json.X509CertificateSerDes` → `org.eclipse.leshan.demo.servers.json.X509CertificateSerDes`\n- Updated StringUtils import:\n  - `org.apache.commons.lang.StringUtils` → `org.apache.commons.lang3.StringUtils`\n\n### Webapp Changes

#### package.json\n- Fixed package name typo: `unplugin-vue-components ` → `unplugin-vue-components`\n- Added `sbom_maven` script placeholder\n\n#### vite.config.js\n- Updated module alias:\n  - `@leshan-server-core-demo` → `@leshan-demo-servers-shared`\n\n#### Vue Components\n- Updated imports to use new module alias:\n  - `@leshan-server-core-demo/components/security/SecurityInfoDialog.vue` → `@leshan-demo-servers-shared/components/security/SecurityInfoDialog.vue`\n  - `@leshan-server-core-demo/components/security/SecurityInfoChip.vue` → `@leshan-demo-servers-shared/components/security/SecurityInfoChip.vue`\n  - `@leshan-server-core-demo/components/path/PathsInput.vue` → `@leshan-demo-servers-shared/components/path/PathsInput.vue`\n\n#### New Files\n- Added `iot-gateway/webapp/src/views/Server.vue` - Server configuration view component\n\n### Build Changes\n\n- Added exclusions for Netty dependencies in hivemq-mqtt-client to resolve dependency convergence issues\n- Updated formatter and checkstyle configurations\n\n### Migration Notes

1. **Security Store**: The `FileSecurityStore` class has been moved to `org.eclipse.leshan.servers.security` package.\n\n2. **Servlet API**: All servlet-related imports have been updated from `javax.servlet` to `jakarta.servlet` (Jakarta EE 10).\n\n3. **Jetty**: EventSource classes have been moved to `org.eclipse.jetty.ee10.servlets` package.\n\n4. **Demo Modules**: Demo-related classes have been reorganized into `leshan-demo-shared` and `leshan-demo-servers-shared` modules.\n\n5. **Webapp**: The Vue.js webapp has been updated to use the new module structure and component imports.\n\n### Testing

After upgrading, the following should be verified:\n- Device registration and deregistration\n- MQTT bridge connectivity\n- Web UI functionality\n- Security store persistence\n- Redis integration (if used)\n</parameter>
</function>
</tool_call>
<tool_call>
<function=exec_shell_command>
<parameter=command>
cd /home/simon/proj/schauer_iot/repos/enro-iot-leshan && mvn package -DskipTests=true 2>&1 | tail -50