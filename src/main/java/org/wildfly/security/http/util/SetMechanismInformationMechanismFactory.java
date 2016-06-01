/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.security.http.util;

import static org.wildfly.security.http.HttpConstants.HOST;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.wildfly.security.auth.callback.MechanismInformationCallback;
import org.wildfly.security.http.HttpAuthenticationException;
import org.wildfly.security.http.HttpServerAuthenticationMechanism;
import org.wildfly.security.http.HttpServerAuthenticationMechanismFactory;
import org.wildfly.security.http.HttpServerRequest;

/**
 * A wrapper {@link HttpServerAuthenticationMechanismFactory} to ensure that mechanism information for the current
 * authentication request is set before the first authentication callbacks.
 *
 * The host name and protocol are derived from the current request being authenticated.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SetMechanismInformationMechanismFactory implements HttpServerAuthenticationMechanismFactory {

    private HttpServerAuthenticationMechanismFactory delegate;

    public SetMechanismInformationMechanismFactory(final HttpServerAuthenticationMechanismFactory delegate) {
        this.delegate = delegate;
    }

    @Override
    public String[] getMechanismNames(Map<String, ?> properties) {
        return delegate.getMechanismNames(properties);
    }

    @Override
    public HttpServerAuthenticationMechanism createAuthenticationMechanism(final String mechanismName, Map<String, ?> properties,
            final CallbackHandler callbackHandler) throws HttpAuthenticationException {
        final HttpServerAuthenticationMechanism mechanism = delegate.createAuthenticationMechanism(mechanismName, properties, callbackHandler);
        return mechanism != null ? new HttpServerAuthenticationMechanism() {

            @Override
            public String getMechanismName() {
                return mechanism.getMechanismName();
            }

            @Override
            public void evaluateRequest(HttpServerRequest request) throws HttpAuthenticationException {
                String host = request.getFirstRequestHeaderValue(HOST);
                String resolvedHostName = null;
                if (host != null) {
                  if (host.startsWith("[")) {
                      int close = host.indexOf(']');
                      if (close > 0) {
                          resolvedHostName = host.substring(0, close);
                      }
                  } else {
                      int colon = host.lastIndexOf(':');
                      resolvedHostName = colon > 0 ? host.substring(0, colon -1) : host;
                  }
                }

                try {
                    callbackHandler.handle(new Callback[] { new MechanismInformationCallback("HTTP", getMechanismName(),
                            resolvedHostName, request.getRequestURI().getScheme()) });
                } catch (IOException e) {
                    throw new HttpAuthenticationException(e);
                } catch (UnsupportedCallbackException ignored) {
                }

                mechanism.evaluateRequest(request);
            }
        } : null;
    }



}
