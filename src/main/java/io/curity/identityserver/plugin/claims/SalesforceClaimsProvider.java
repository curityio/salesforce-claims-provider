/*
 * Copyright (C) 2020 Curity AB. All rights reserved.
 *
 * The contents of this file are the property of Curity AB.
 * You may not copy or use this file, in either source code
 * or executable form, except in compliance with terms
 * set by Curity AB.
 *
 * For further information, please contact Curity AB.
 */

package io.curity.identityserver.plugin.claims;

import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.curity.identityserver.sdk.attribute.Attributes;
import se.curity.identityserver.sdk.attribute.AuthenticationAttributes;
import se.curity.identityserver.sdk.claims.ClaimsProvider;
import se.curity.identityserver.sdk.claims.RequestedClaimAttribute;
import se.curity.identityserver.sdk.data.authorization.ScopeValue;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.oauth.OAuthClient;
import se.curity.identityserver.sdk.service.*;
import se.curity.identityserver.sdk.service.crypto.AsymmetricSigningCryptoStore;

import java.net.URI;
import java.util.Optional;
import java.util.Set;

public final class SalesforceClaimsProvider implements ClaimsProvider
{
    private final SalesforceClaimsProviderConfig _config;
    private final WebServiceClientFactory _webServiceClientFactory;
    private final ExceptionFactory _exceptionFactory;
    private final AsymmetricSigningCryptoStore _signingKeyStore;
    private final Json _json;

    private final SalesforceClaimsProviderManagedObject _tokenCache;

    private final static Logger _logger = LoggerFactory.getLogger(SalesforceClaimsProvider.class);

    public SalesforceClaimsProvider(
            SalesforceClaimsProviderConfig config,
            SalesforceClaimsProviderManagedObject tokenCache)
    {
        _config = config;
        _webServiceClientFactory = _config.getWebServiceClientFactory();
        _json = _config.getJson();
        _exceptionFactory = _config.getExceptionFactory();
        _tokenCache = tokenCache;
        _signingKeyStore = _config.getSigningKeyStore();
    }

    @Override
    public Attributes getClaimValues(Set<RequestedClaimAttribute> requestedClaimAttributes,
                                     Set<ScopeValue> scopeValues,
                                     AuthenticationAttributes userAuthenticationAttributes,
                                     OAuthClient client)
    {
        try
        {
            return getUserClaims(userAuthenticationAttributes);
        }
        catch (JoseException e)
        {
            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
    }

    private String getToken() throws JoseException {
        return _tokenCache.getToken(_signingKeyStore.getPrivateKey(), getWebServiceClient(), _config.getJson());
    }

    private String getNewToken() throws JoseException {
        return _tokenCache.getNewToken(_signingKeyStore.getPrivateKey(), getWebServiceClient(), _config.getJson());
    }

    /** Call the Salesforce REST API to retrieve user claim data */
    private Attributes getUserClaims(AuthenticationAttributes userLoginAttributes) throws JoseException {
        HttpResponse response = callEndpoint(getToken(), userLoginAttributes);

        int statusCode = response.statusCode();

        if(statusCode == 401)
        {
            _logger.warn("Got error response from Salesforce REST API: error = {}, {}", statusCode,
                        response.body(HttpResponse.asString()));

            response = callEndpoint(getNewToken(), userLoginAttributes); //Got 401 using cached token, requesting new token and calling Salesforce API again
            statusCode = response.statusCode();
        }

        if (statusCode != 200)
        {
            if (_logger.isDebugEnabled())
            {
                _logger.debug("Got error response from Salesforce REST API: error = {}, {}", statusCode,
                        response.body(HttpResponse.asString()));
            }

            throw _exceptionFactory.internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }
        return _json.toAttributes(response.body(HttpResponse.asString()));
    }

    private HttpResponse callEndpoint(String token, AuthenticationAttributes userLoginAttributes)
    {
        String configQuery = _config.getQuery();
        _logger.trace("Configured query: " + configQuery);
        String[] parts = configQuery.split(":");

        String userKey;
        String query;

        if(parts.length == 2) {
            userKey = parts[1];
            _logger.trace("User key: " + userKey);
            query = parts[0];

            if(userKey != null)
            {
                return getWebServiceClient()
                        .withPath(_config.getDataPath())
                        .withQuery(query + "'" + userLoginAttributes.getSubjectAttributes().get(userKey).getValue() + "'")
                        .request()
                        .contentType("application/json")
                        .accept("application/json")
                        .header("Authorization", "Bearer " + token)
                        .method("GET")
                        .response();
            }

            throw _exceptionFactory.configurationException("Missing key in Salesforce query");
        }

        throw _exceptionFactory.configurationException("Salesforce query not correctly configured");
    }

    private WebServiceClient getWebServiceClient()
    {
        Optional<HttpClient> httpClient = _config.getHttpClient();

        if (httpClient.isPresent())
        {
            return _webServiceClientFactory.create(httpClient.get()).withHost(_config.getHost());
        }
        else
        {
            return _webServiceClientFactory.create(URI.create("https://login.salesforce.com"));
        }
    }
}
