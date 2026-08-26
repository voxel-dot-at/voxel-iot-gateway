package at.voxel.iotgw.lwm2m;

import java.io.File;
import java.net.InetSocketAddress;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.util.CertPathUtil;
import org.eclipse.californium.scandium.config.DtlsConfig;
import org.eclipse.californium.scandium.config.DtlsConfig.DtlsRole;

import org.eclipse.leshan.core.endpoint.DefaultEndPointUriHandler;
import org.eclipse.leshan.core.endpoint.EndPointUriHandler;
import org.eclipse.leshan.core.endpoint.Protocol;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.eclipse.leshan.demo.LwM2mDemoConstant;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.server.LeshanServerBuilder;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.eclipse.leshan.server.model.VersionedModelProvider;
import org.eclipse.leshan.server.redis.RedisRegistrationStore;
import org.eclipse.leshan.server.redis.RedisSecurityStore;
import org.eclipse.leshan.servers.security.EditableSecurityStore;
import org.eclipse.leshan.transport.californium.PrincipalMdcConnectionListener;
import org.eclipse.leshan.transport.californium.server.endpoint.CaliforniumServerEndpointsProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapOscoreServerEndpointFactory;
import org.eclipse.leshan.transport.californium.server.endpoint.coap.CoapServerProtocolProvider;
import org.eclipse.leshan.transport.californium.server.endpoint.coaps.CoapsServerProtocolProvider;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapTcpServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.coaptcp.endpoint.JavaCoapsTcpServerEndpointsProvider;
import org.eclipse.leshan.transport.javacoap.server.endpoint.JavaCoapServerEndpointsProvider;

import at.voxel.iotgw.cli.IoTGatewayCli;
import at.voxel.iotgw.security.SqliteSecurityStore;

/**
 * LWM2M Server configuration and builder.
 */
public class Lwm2mServer {

    private static final String CF_CONFIGURATION_FILENAME = "Californium3.server.properties";
    private static final String CF_CONFIGURATION_HEADER = "VoXel IoT GW - " + Configuration.DEFAULT_HEADER;
    private static final EndPointUriHandler uriHandler = new DefaultEndPointUriHandler();

    /**
     * Creates and configures a LeshanServer instance.
     *
     * @param cli The CLI configuration object
     * @return A configured LeshanServer instance
     * @throws Exception if server creation fails
     */
    public static LeshanServer createServer(IoTGatewayCli cli) throws Exception {
        // Prepare LWM2M server
        LeshanServerBuilder builder = new LeshanServerBuilder();

        // Define model provider
        List<ObjectModel> models = ObjectLoader.loadAllDefault();
        models.addAll(ObjectLoader.loadDdfResources("/models/", LwM2mDemoConstant.modelPaths));
        if (cli.main.modelsFolder != null) {
            models.addAll(ObjectLoader.loadObjectsFromDir(cli.main.modelsFolder, true));
        }
        models.addAll(ObjectLoader.loadObjectsFromDir(new File("models.local")));
        LwM2mModelProvider modelProvider = new VersionedModelProvider(models);
        builder.setObjectModelProvider(modelProvider);

        // Set securityStore & registrationStore
        EditableSecurityStore securityStore;
        if (cli.main.redis == null) {
            // use SQLite persistence
            securityStore = new SqliteSecurityStore();
            System.out.println("Using SqliteSecurityStore for security info persistence");
        } else {
            // use Redis Store
            securityStore = new RedisSecurityStore(cli.main.redis);
            builder.setRegistrationStore(new RedisRegistrationStore(cli.main.redis));
            System.out.println("Using RedisSecurityStore for security info persistence");
        }
        builder.setSecurityStore(securityStore);

        if (cli.identity.isx509()) {
            // use X.509 mode (+ RPK)
            builder.setPrivateKey(cli.identity.getPrivateKey());
            builder.setCertificateChain(cli.identity.getCertChain());

            // Define trust store
            List<Certificate> trustStore = cli.identity.getTrustStore();
            builder.setTrustedCertificates(trustStore.toArray(new Certificate[trustStore.size()]));
        } else if (cli.identity.isRPK()) {
            // use RPK only
            builder.setPublicKey(cli.identity.getPublicKey());
            builder.setPrivateKey(cli.identity.getPrivateKey());
        }

        // Create Californium Endpoints Provider:
        // ------------------
        // Create Server Endpoints Provider
        CaliforniumServerEndpointsProvider.Builder endpointsBuilder = new CaliforniumServerEndpointsProvider.Builder(
                // Add coap Protocol support
                new CoapServerProtocolProvider(),

                // Add coaps protocol support
                new CoapsServerProtocolProvider(c -> {
                    // Add MDC for connection logs
                    if (cli.helpsOptions.getVerboseLevel() > 0)
                        c.setConnectionListener(new PrincipalMdcConnectionListener());

                }));

        // Create Californium Configuration
        Configuration serverCoapConfig = endpointsBuilder.createDefaultConfiguration();

        // Set some DTLS stuff
        // These configuration values are always overwritten by CLI therefore set them
        // to transient.
        serverCoapConfig.setTransient(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY);
        serverCoapConfig.set(DtlsConfig.DTLS_RECOMMENDED_CIPHER_SUITES_ONLY, !cli.dtls.supportDeprecatedCiphers);
        serverCoapConfig.setTransient(DtlsConfig.DTLS_CONNECTION_ID_LENGTH);
        if (cli.dtls.cid != null) {
            serverCoapConfig.set(DtlsConfig.DTLS_CONNECTION_ID_LENGTH, cli.dtls.cid);
        }

        // Persist configuration
        File configFile = new File(CF_CONFIGURATION_FILENAME);
        if (configFile.isFile()) {
            serverCoapConfig.load(configFile);
        } else {
            serverCoapConfig.store(configFile, CF_CONFIGURATION_HEADER);
        }

        // Enforce DTLS role to ServerOnly if needed
        if (cli.identity.isx509()) {
            X509Certificate serverCertificate = cli.identity.getCertChain()[0];
            if (serverCoapConfig.get(DtlsConfig.DTLS_ROLE) == DtlsRole.BOTH) {
                if (serverCertificate != null) {
                    if (CertPathUtil.canBeUsedForAuthentication(serverCertificate, false)) {
                        if (!CertPathUtil.canBeUsedForAuthentication(serverCertificate, true)) {
                            serverCoapConfig.set(DtlsConfig.DTLS_ROLE, DtlsRole.SERVER_ONLY);
                            System.out.println("Server certificate does not allow Client Authentication usage."
                                    + "\nThis will prevent this LWM2M server to initiate DTLS connection."
                                    + "\nSee : https://github.com/eclipse/leshan/wiki/Server-Failover#about-connections");
                        }
                    }
                }
            }
        }

        // Set Californium Configuration
        endpointsBuilder.setConfiguration(serverCoapConfig);

        // Create CoAP endpoint
        int coapPort = cli.main.localPort == null ? serverCoapConfig.get(CoapConfig.COAP_PORT) : cli.main.localPort;
        InetSocketAddress coapAddr = cli.main.localAddress == null ? new InetSocketAddress(coapPort)
                : new InetSocketAddress(cli.main.localAddress, coapPort);
        if (cli.main.disableOscore) {
            endpointsBuilder.addEndpoint(coapAddr, Protocol.COAP);
        } else {
            endpointsBuilder.addEndpoint(
                    new CoapOscoreServerEndpointFactory(uriHandler.createUri(Protocol.COAP.getUriScheme(), coapAddr)));
        }

        // Create CoAP over DTLS endpoint
        int coapsPort = cli.main.secureLocalPort == null ? serverCoapConfig.get(CoapConfig.COAP_SECURE_PORT)
                : cli.main.secureLocalPort;
        InetSocketAddress coapsAddr = cli.main.secureLocalAddress == null ? new InetSocketAddress(coapsPort)
                : new InetSocketAddress(cli.main.secureLocalAddress, coapsPort);
        endpointsBuilder.addEndpoint(coapsAddr, Protocol.COAPS);

        // Create CoAP endpoint based on java-coap
        int jcoapPort = cli.main.jlocalPort;
        InetSocketAddress jcoapAddr = cli.main.jlocalAddress == null ? new InetSocketAddress(jcoapPort)
                : new InetSocketAddress(cli.main.jlocalAddress, jcoapPort);
        JavaCoapServerEndpointsProvider javacoapEndpointsProvider = new JavaCoapServerEndpointsProvider(jcoapAddr);

        // Create CoAP over TCP endpoint based on java-coap
        int coapTcpPort = cli.main.jTcpLocalPort;
        InetSocketAddress coapTcpAddr = cli.main.jTcpLocalAddress == null ? new InetSocketAddress(coapTcpPort)
                : new InetSocketAddress(cli.main.jTcpLocalAddress, coapTcpPort);
        JavaCoapTcpServerEndpointsProvider javacoapTcpEndpointsProvider = new JavaCoapTcpServerEndpointsProvider(
                coapTcpAddr);

        // Create CoAP over TLS endpoint based on java-coap
        int coapsTcpPort = cli.main.jTlsLocalPort;
        InetSocketAddress coapsTcpAddr = cli.main.jTlsLocalAddress == null ? new InetSocketAddress(coapsTcpPort)
                : new InetSocketAddress(cli.main.jTlsLocalAddress, coapTcpPort);
        JavaCoapsTcpServerEndpointsProvider javacoapsTcpEndpointsProvider = new JavaCoapsTcpServerEndpointsProvider(
                coapsTcpAddr);

        // Create LWM2M server
        builder.setEndpointsProviders(endpointsBuilder.build(), javacoapEndpointsProvider, javacoapTcpEndpointsProvider,
                javacoapsTcpEndpointsProvider);
        return builder.build();
    }
}
