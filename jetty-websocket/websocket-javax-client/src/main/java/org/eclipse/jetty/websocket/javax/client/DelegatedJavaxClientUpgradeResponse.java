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

package org.eclipse.jetty.websocket.javax.client;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.websocket.core.ExtensionConfig;
import org.eclipse.jetty.websocket.javax.common.UpgradeResponse;

/**
 * Representing the Jetty {@link org.eclipse.jetty.client.HttpClient}'s {@link HttpResponse}
 * in the {@link UpgradeResponse} interface.
 */
public class DelegatedJavaxClientUpgradeResponse implements UpgradeResponse
{
    private HttpResponse delegate;

    public DelegatedJavaxClientUpgradeResponse(HttpResponse response)
    {
        this.delegate = response;
    }

    @Override
    public String getAcceptedSubProtocol()
    {
        return this.delegate.getHeaders().get(HttpHeader.SEC_WEBSOCKET_SUBPROTOCOL);
    }

    @Override
    public List<ExtensionConfig> getExtensions()
    {
        List<String> rawExtensions = delegate.getHeaders().getValuesList(HttpHeader.SEC_WEBSOCKET_EXTENSIONS);
        if (rawExtensions == null || rawExtensions.isEmpty())
            return Collections.emptyList();

        return rawExtensions.stream().map((parameterizedName) -> ExtensionConfig.parse(parameterizedName)).collect(Collectors.toList());
    }
}
