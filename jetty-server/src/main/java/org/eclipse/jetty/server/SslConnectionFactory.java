//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.server;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.io.AbstractConnection;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.ssl.SslConnection;
import org.eclipse.jetty.io.ssl.SslHandshakeListener;
import org.eclipse.jetty.util.annotation.Name;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class SslConnectionFactory extends AbstractConnectionFactory
{
    private final SslContextFactory.Server _sslContextFactory;
    private final String _nextProtocol;
    private boolean _directBuffersForEncryption = false;
    private boolean _directBuffersForDecryption = false;

    public SslConnectionFactory()
    {
        this(HttpVersion.HTTP_1_1.asString());
    }

    public SslConnectionFactory(@Name("next") String nextProtocol)
    {
        this(null, nextProtocol);
    }

    public SslConnectionFactory(@Name("sslContextFactory") SslContextFactory.Server factory, @Name("next") String nextProtocol)
    {
        super("SSL");
        _sslContextFactory = factory == null ? new SslContextFactory.Server() : factory;
        _nextProtocol = nextProtocol;
        addBean(_sslContextFactory);
    }

    public SslContextFactory.Server getSslContextFactory()
    {
        return _sslContextFactory;
    }

    public void setDirectBuffersForEncryption(boolean useDirectBuffers)
    {
        this._directBuffersForEncryption = useDirectBuffers;
    }

    public void setDirectBuffersForDecryption(boolean useDirectBuffers)
    {
        this._directBuffersForDecryption = useDirectBuffers;
    }

    public boolean isDirectBuffersForDecryption()
    {
        return _directBuffersForDecryption;
    }

    public boolean isDirectBuffersForEncryption()
    {
        return _directBuffersForEncryption;
    }

    public String getNextProtocol()
    {
        return _nextProtocol;
    }

    @Override
    protected void doStart() throws Exception
    {
        super.doStart();

        SSLEngine engine = _sslContextFactory.newSSLEngine();
        engine.setUseClientMode(false);
        SSLSession session = engine.getSession();

        if (session.getPacketBufferSize() > getInputBufferSize())
            setInputBufferSize(session.getPacketBufferSize());
    }

    @Override
    public Connection newConnection(Connector connector, EndPoint endPoint)
    {
        SSLEngine engine = _sslContextFactory.newSSLEngine(endPoint.getRemoteAddress());
        engine.setUseClientMode(false);

        SslConnection sslConnection = newSslConnection(connector, endPoint, engine);
        sslConnection.setRenegotiationAllowed(_sslContextFactory.isRenegotiationAllowed());
        sslConnection.setRenegotiationLimit(_sslContextFactory.getRenegotiationLimit());
        configure(sslConnection, connector, endPoint);

        ConnectionFactory next = connector.getConnectionFactory(_nextProtocol);
        EndPoint decryptedEndPoint = sslConnection.getDecryptedEndPoint();
        Connection connection = next.newConnection(connector, decryptedEndPoint);
        decryptedEndPoint.setConnection(connection);

        return sslConnection;
    }

    protected SslConnection newSslConnection(Connector connector, EndPoint endPoint, SSLEngine engine)
    {
        return new SslConnection(connector.getByteBufferPool(), connector.getExecutor(), endPoint, engine, isDirectBuffersForEncryption(), isDirectBuffersForDecryption());
    }

    @Override
    protected AbstractConnection configure(AbstractConnection connection, Connector connector, EndPoint endPoint)
    {
        if (connection instanceof SslConnection)
        {
            SslConnection sslConnection = (SslConnection)connection;
            if (connector instanceof ContainerLifeCycle)
            {
                ContainerLifeCycle container = (ContainerLifeCycle)connector;
                container.getBeans(SslHandshakeListener.class).forEach(sslConnection::addHandshakeListener);
            }
            getBeans(SslHandshakeListener.class).forEach(sslConnection::addHandshakeListener);
        }
        return super.configure(connection, connector, endPoint);
    }

    @Override
    public String toString()
    {
        return String.format("%s@%x{%s->%s}", this.getClass().getSimpleName(), hashCode(), getProtocol(), _nextProtocol);
    }
}
