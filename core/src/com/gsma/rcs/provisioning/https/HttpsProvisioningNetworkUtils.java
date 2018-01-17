/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2017 China Mobile.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.provisioning.https;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.telephony.TelephonyFactory;
import com.gsma.rcs.utils.logger.Logger;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class HttpsProvisioningNetworkUtils {
    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(HttpsProvisioningNetworkUtils.class
            .getName());

    private static final HostnameVerifier NON_HOSTNAME_VERIFIER = new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            sLogger.debug("HostnameVerifier host=" + hostname + ", peer=" + session.getPeerHost());
            return true;
        }
    };

    private static final TrustManager[] ALLCERTS_TRUST_MANAGER = new TrustManager[] {

        new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                sLogger.debug("checkClientTrusted");
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                sLogger.debug("checkServerTrusted");
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }
        }
    };

    /**
     * Trust every server - don't check for any certificate
     */
    public static void trustAllHosts() {
        try {
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, ALLCERTS_TRUST_MANAGER, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(NON_HOSTNAME_VERIFIER);
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            sLogger.error("Failed to install all-trusting trust manager", e);
        }
    }

    // TODO realize this method to run provisioning http requests on PS silently
    @TargetApi(Build.VERSION_CODES.M)
    public static void executeRequestOnMobile(final String request) {
        Context context = AndroidFactory.getApplicationContext();
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        builder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        builder.addCapability(NetworkCapabilities.NET_CAPABILITY_RCS);
        // builder.addCapability(NetworkCapabilities.NET_CAPABILITY_IMS);
        builder.setNetworkSpecifier(Long.toString(TelephonyFactory.getFactory()
                .getDefaultDataSubId()));
        NetworkRequest networkRequest = builder.build();
        context.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL",
                "ProvisioningService");
        ConnectivityManager.NetworkCallback callback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {

            }
        };
        connectivityManager.requestNetwork(networkRequest, callback);
    }
}
