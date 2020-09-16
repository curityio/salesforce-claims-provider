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

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.lang.JoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.curity.identityserver.sdk.Nullable;
import se.curity.identityserver.sdk.errors.ErrorCode;
import se.curity.identityserver.sdk.http.HttpRequest;
import se.curity.identityserver.sdk.http.HttpResponse;
import se.curity.identityserver.sdk.plugin.ManagedObject;
import se.curity.identityserver.sdk.service.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SalesforceClaimsProviderManagedObject extends ManagedObject<SalesforceClaimsProviderConfig>
{
    private static final Logger _logger = LoggerFactory.getLogger(SalesforceClaimsProviderManagedObject.class);
    private final SalesforceClaimsProviderConfig _config;
    private PrivateKey _signingKey;

    @Nullable
    private final AtomicReference<String> _token = new AtomicReference<>(null);

    public SalesforceClaimsProviderManagedObject(SalesforceClaimsProviderConfig configuration)
    {
        super(configuration);
        _config = configuration;
    }

    /** Returns the cached token or retrieves a new token from Salesforce if no token is available */
    public synchronized String getToken(PrivateKey signingKey, WebServiceClient client, Json json) throws JoseException {
        _signingKey = signingKey;

        //if no token is acquired yet
        if(_token.get() == null){
            requestAccessToken(createJWT(), client, json);
        }

        return _token.get();
    }

    /** Retrieves a new Access Token from Salesforce and returns it */
    public synchronized String getNewToken(PrivateKey signingKey, WebServiceClient client, Json json) throws JoseException {
        _signingKey = signingKey;

        requestAccessToken(createJWT(), client, json);

        return _token.get();
    }

    /** Create and sign the JWT to send to Salesforce for token retrieval */
    private String createJWT() throws JoseException
    {
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(_config.getConsumerKey());
        claims.setAudience("https://login.salesforce.com");
        claims.setExpirationTimeMinutesInTheFuture(3); // time when the token will expire (3 minutes from now)
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setSubject(_config.getPrincipal()); // the subject/principal is whom the token is about

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());

        jws.setKey(_signingKey);
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);

        return jws.getCompactSerialization();
    }

    /** Call the Salesforce token endpoint with the signed JWT to retrieve an Access Token */
    private void requestAccessToken(String jwt, WebServiceClient client, Json json) {
        HttpResponse tokenResponse = client
                .withPath(_config.getTokenEndpoint())
                .request()
                .timeout(Duration.ofSeconds(10))
                .contentType("application/x-www-form-urlencoded")
                .body(getFormEncodedBodyFrom(createPostData(jwt)))
                .method("POST")
                .response();
        int statusCode = tokenResponse.statusCode();

        if (statusCode != 200)
        {
            if (_logger.isInfoEnabled())
            {
                _logger.info("Got error response from token endpoint: error = {}, {}", statusCode,
                        tokenResponse.body(HttpResponse.asString()));
            }

            throw _config.getExceptionFactory().internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR);
        }

        Map<String, Object> responseMap = json.fromJson(tokenResponse.body(HttpResponse.asString()));

        if (responseMap.containsKey("access_token"))
        {
            _token.set(responseMap.get("access_token").toString());
        }
        else
        {
            throw _config.getExceptionFactory().internalServerException(ErrorCode.EXTERNAL_SERVICE_ERROR, "No 'access_token' in Salesforce response");
        }
    }

    private static Map<String, String> createPostData(String assertion)
    {
        Map<String, String> data = new HashMap<>(5);

        data.put("assertion", assertion);
        data.put("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");

        return data;
    }

    private static HttpRequest.BodyProcessor getFormEncodedBodyFrom(Map<String, String> data) {
        StringBuilder stringBuilder = new StringBuilder();
        boolean first = true;
        String key, value, encodedKey;

        for(Map.Entry<String, String> entry : data.entrySet()){
            if (first)
            {
                first = false;
            }
            else
            {
                stringBuilder.append("&"); //Only add & if its not the first entry
            }

            key = entry.getKey();
            value = entry.getValue();
            encodedKey = urlEncodeString(key);
            stringBuilder.append(encodedKey);

            if (!Objects.isNull(value))
            {
                String encodedValue = urlEncodeString(value);
                stringBuilder.append("=").append(encodedValue);
            }
        }

        return HttpRequest.fromString(stringBuilder.toString(), StandardCharsets.UTF_8);
    }

    private static String urlEncodeString(String unencodedString)
    {
        try
        {
            return URLEncoder.encode(unencodedString, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("This server cannot support UTF-8!", e);
        }
    }

    @Override
    public void close() throws IOException
    {
        super.close();
    }
}