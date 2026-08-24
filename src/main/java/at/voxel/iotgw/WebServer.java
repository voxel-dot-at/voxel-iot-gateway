/*******************************************************************************
 * Copyright (c) 2023-2024 VoXel Interaction Design.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/documents/edl-v10.html.
 *
 * Contributors:
 *     Simon Vogl - initial implementation
 *******************************************************************************/
package at.voxel.iotgw;

import java.net.InetSocketAddress;

import org.eclipse.jetty.ee10.servlet.DefaultServlet;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.leshan.demo.servers.json.servlet.SecurityServlet;
import org.eclipse.leshan.demo.servers.json.servlet.ServerServlet;
import org.eclipse.leshan.server.LeshanServer;
import org.eclipse.leshan.servers.security.EditableSecurityStore;

import at.voxel.iotgw.cli.IoTGatewayCli;
import at.voxel.iotgw.servlet.ClientServlet;
import at.voxel.iotgw.servlet.EventServlet;
import at.voxel.iotgw.servlet.ObjectSpecServlet;

/**
 * Web server implementation using Jetty.
 */
public class WebServer extends Server {
    private final org.eclipse.jetty.server.Server jettyServer;
    private final IoTGatewayCli cli;
    private final LeshanServer lwServer;

    public WebServer(IoTGatewayCli cli, LeshanServer lwServer) {
        this.cli = cli;
        this.lwServer = lwServer;
        
        InetSocketAddress jettyAddr;
        if (cli.main.webhost == null) {
            jettyAddr = new InetSocketAddress(cli.main.webPort);
        } else {
            jettyAddr = new InetSocketAddress(cli.main.webhost, cli.main.webPort);
        }
        System.out.printf("Web Listening on port %d%n", cli.main.webPort);

        this.jettyServer = new org.eclipse.jetty.server.Server(jettyAddr);
        ServletContextHandler root = new ServletContextHandler("/", true, false);
        this.jettyServer.setHandler(root);

        // Create static Servlet
        DefaultServlet staticServelt = new DefaultServlet();
        ServletHolder staticHolder = new ServletHolder(staticServelt);
        staticHolder.setInitParameter("baseResource",
                SimpleIoTGateway.class.getClassLoader().getResource("webapp").toExternalForm());
        staticHolder.setInitParameter("gzip", "true");
        staticHolder.setInitParameter("cacheControl", "public, max-age=31536000");
        root.addServlet(staticHolder, "/");

        // Create REST API Servlets
        EventServlet eventServlet = new EventServlet(lwServer);
        ServletHolder eventServletHolder = new ServletHolder(eventServlet);
        root.addServlet(eventServletHolder, "/api/event/*");

        ServletHolder clientServletHolder = new ServletHolder(new ClientServlet(lwServer, eventServlet));
        root.addServlet(clientServletHolder, "/api/clients/*");

        ServletHolder securityServletHolder;
        if (cli.identity.isRPK()) {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getPublicKey()));
        } else {
            securityServletHolder = new ServletHolder(new SecurityServlet(
                    (EditableSecurityStore) lwServer.getSecurityStore(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(securityServletHolder, "/api/security/*");

        ServletHolder serverServletHolder;
        if (cli.identity.isRPK()) {
            serverServletHolder = new ServletHolder(
                    new ServerServlet(lwServer.getEndpoints(), cli.identity.getPublicKey()));
        } else {
            serverServletHolder = new ServletHolder(
                    new ServerServlet(lwServer.getEndpoints(), cli.identity.getCertChain()[0]));
        }
        root.addServlet(serverServletHolder, "/api/server/*");

        ServletHolder objectSpecServletHolder = new ServletHolder(
                new ObjectSpecServlet(lwServer.getModelProvider(), lwServer.getRegistrationService()));
        root.addServlet(objectSpecServletHolder, "/api/objectspecs/*");
    }

    @Override
    public void start() throws Exception {
        jettyServer.start();
        running = true;
    }

    @Override
    public void stop() throws Exception {
        jettyServer.stop();
        running = false;
    }
}
