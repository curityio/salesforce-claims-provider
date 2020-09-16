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

import se.curity.identityserver.sdk.config.Configuration;
import se.curity.identityserver.sdk.config.annotation.DefaultString;
import se.curity.identityserver.sdk.config.annotation.Description;
import se.curity.identityserver.sdk.config.annotation.Name;
import se.curity.identityserver.sdk.service.*;
import se.curity.identityserver.sdk.service.crypto.AsymmetricSigningCryptoStore;

import java.util.Optional;

@Name("salesforce-claims-provider")
public interface SalesforceClaimsProviderConfig extends Configuration
{
    @Description("The http client to use for this claims provider")
    Optional<HttpClient> getHttpClient();

    @Description("The Salesforce App Consumer Key")
    String getConsumerKey();

    @Description("The Salesforce host, e.g. yourInstance.salesforce.com")
//    @DefaultString("yourInstance.salesforce.com")
    String getHost();

    @Description("The Salesforce path to retrieve user data")
    @DefaultString("/services/data/v48.0/query/")
    String getDataPath();

    @Description("The Salesforce token endpoint")
    @DefaultString("/services/oauth2/token")
    String getTokenEndpoint();

    @Description("The Salesforce REST API query. It should contain a statement that marks the subject as :subject to be the replaced variable. " +
            "Example: q=SELECT+department,title+from+Contact+WHERE+email= :email. :email will be mapped against the value given when the query is executed.")
    @DefaultString("q=SELECT+department,title+from+Contact+WHERE+email=:email")
    String getQuery();

    @Description("The Salesforce user allowed to query the REST API")
    String getPrincipal();

    WebServiceClientFactory getWebServiceClientFactory();
    ExceptionFactory getExceptionFactory();
    Json getJson();

    @Description("A reference to a Signing Keystore with an asymmetric key. The key signs the JWT that is used to retrieve an Access Token from Salesforce.")
    AsymmetricSigningCryptoStore getSigningKeyStore();
}
