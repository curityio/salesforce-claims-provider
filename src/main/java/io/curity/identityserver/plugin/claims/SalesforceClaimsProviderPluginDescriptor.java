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

import se.curity.identityserver.sdk.claims.ClaimsProvider;
import se.curity.identityserver.sdk.plugin.descriptor.ClaimsProviderPluginDescriptor;

import java.util.Optional;

public final class SalesforceClaimsProviderPluginDescriptor implements ClaimsProviderPluginDescriptor<SalesforceClaimsProviderConfig>
{
    @Override
    public Class<? extends ClaimsProvider> getClaimsProvider()
    {
        return SalesforceClaimsProvider.class;
    }

    @Override
    public String getPluginImplementationType()
    {
        return "salesforce-claims-provider";
    }

    @Override
    public Class<SalesforceClaimsProviderConfig> getConfigurationType()
    {
        return SalesforceClaimsProviderConfig.class;
    }

    @Override
    public Optional<SalesforceClaimsProviderManagedObject> createManagedObject(SalesforceClaimsProviderConfig config)
    {
        return Optional.of(new SalesforceClaimsProviderManagedObject(config));
    }

}
