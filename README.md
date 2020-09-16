# Salesforce Claims Provider

[![Quality](https://img.shields.io/badge/quality-demo-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

This repository contains a Claims Provider that fetches data from the Salesforce REST API using the [JWT bearer token flow](https://help.salesforce.com/articleView?id=remoteaccess_oauth_jwt_flow.htm&type=5).

## System Requirements

Curity Identity Server 5.0.0 or later and its [system requirements](https://developer.curity.io/docs/latest/system-admin-guide/system-requirements.html).

## Build and deploy the Plugin

* Run mvn package
* Create a directory in /usr/share/plugins/. Ex. salesforce
* Copy the generated .jar files in /target/usr/share/plugins/salesforce/ to the newly created folder /usr/share/plugins/salesforce/
* Restart the Curity Identity Server

## Configuration

### Salesforce configuration

#### Creating a Connected App
* Details on creating a [Connected App in Salesforce](https://help.salesforce.com/articleView?id=connected_app_create.htm&type=5) is described in the Salesforce developer documentation.
* Export certificate from the Curity Identity Server and import in the Connected App in Salesforce. In Curity, go to `Facilities` -> `Crypto` -> `Signing Keys` and select the key that is going to be used by the Salesforce Claims Provider (see below). Then click `Download PEM` in the top right corner.
In the `API (Enable OAuth Settings)` section in Salesforce, check the box `Use digital signatures` and click `Choose File` to upload the PEM file.
* In the `API (Enable OAuth Settings)` section of the Connected App select the needed scopes. `Perform requests on your behalf at any time (refresh_token, offline_access)` is required and `Access and manage your data (api)` or `Full access (full)` would work. Others might work depending on the Salesforce environment.

#### Managing the Connected App

The Connected App needs to be configured to allow Curity to query the REST API for data. A service account user should be used for this purpose.

* Manage the created Connected App in Salesforce. `Setup` -> `Platform Tools` -> `Apps` -> `App Manager`. For the previously created Connected App, click the arrow pointing down on the right and choose `Manage`.
* At the top, click `Edit Policies`.
* For `Permitted Users` choose `Admin approved users are pre-authorized`.
* Choose the appropriate option for `IP Relaxation` and click `Save`
* In the `Profiles` section, click `Manage Profiles`. Choose the appropriate Profiles that should have access to query the Salesforce REST API.
* Assign appropriate permissions for the user that is used in Curity to query the API. The Profile selected here should correlate to the Profile assigned to the service account user that is configured in the Claims Provider in Curity (see below).

### Configuring the Claims Provider Plugin in Curity

Parameter | Description                                           | Example
----------|-------------------------------------------------------|----
HttpClient| Configure a simple HttpClient with no authentication configured | `salesforce-http-client`
Signing Key Store | The Claims Provider needs a key to sign a JWT that is sent to the Salesforce token endpoint. Salesforce validates the signed JWT using the certificate uploaded to the Salesforce Connected App. | `salesforce-signing-key`
Consumer Key | This is the `Consumer Key` from the Connected App in Salesforce. | `8NVG96hCUx1bhPauuWSU6_vgbbmTg.FjZD6L9.a5ZP_wp8R_z6Ba6PDfDVqktspT760hRvvIcctaT2PqnYFVT`
Data Path | The Salesforce REST API path that is used to fetch the data requested in the query. | `/services/data/v48.0/query/`
Host | The Salesforce host name. This might need to be set if custom domain is enforced in Salesforce and authentication not allowed from the default `login.salesforce.com`. If not set in the configuration this defaults to `login.salesforce.com` | `login.salesforce.com`
Principal | The Salesforce user that the Claims Provider executes the query on behalf of. This user needs to be given appropriate permissions to the Connected App. | `user@example.com`
Query | The query to execute to retrieve data from Salesforce. The user email will be added to the end of the query. | `q=SELECT+department,title+from+Contact+WHERE+email=`
Token Endpoint | The Salesforce token endpoint that the Claims Provider will use to request and access token for the REST API. | `/services/oauth2/token`

### Configure a Claim

When creating a claim the response from Salesforce needs to be transformed depending on the query result. For the example query `q=SELECT+department,title+from+Contact+WHERE+email=` the below mapping would be used to map the department value to a claim.

```javascript
function transform(records) {
    return records.records[0].Department;
}
```

Add screenshot here instead?

## Troubleshooting

Error | Solution
------|--------
{"error":"invalid_request","error_description":"refresh_token scope is required and the connected app should be installed and preauthorized."} | This occurs if the scope `Perform requests on your behalf at any time (refresh_token, offline_access)` is missing in the `API (Enable OAuth Settings)` of the `Connected App` in Salesforce.
{"message":"This session is not valid for use with the REST API","errorCode":"INVALID_SESSION_ID"} | This occurs if there are not enough scopes configured in the `API (Enable OAuth Settings)` of the `Connected App` in Salesforce. `Access and manage your data (api)`, `Full access (full)` or other might work depending on the Salesforce environment used.
{"error":"invalid_app_access","error_description":"user is not admin approved to access this app"} | This occurs when the configured Principal User does not have enough permissions. Check the `Profiles` that are assigned to the Connected App and that the configured Principle User has the correct Profiles.

## Additional Resources
* https://salesforce.stackexchange.com/questions/49633/how-to-specify-admin-approved-users-for-a-connected-app
* https://help.salesforce.com/articleView?id=connected_app_continuous_ip.htm
