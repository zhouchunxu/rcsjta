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

package com.gsma.rcs.core.ims.network.registration;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.protocol.PayloadException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.security.HttpDigestMd5Authentication;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.utils.Base64;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.SimChallengeUtils;
import com.gsma.rcs.utils.StringUtils;

import java.text.ParseException;
import java.util.Map;

import javax2.sip.header.AuthenticationInfoHeader;
import javax2.sip.header.AuthorizationHeader;
import javax2.sip.header.WWWAuthenticateHeader;

/**
 * IMS AKA registration procedure (see RFC3310)
 */
public class ImsAkaRegistrationProcedure extends RegistrationProcedure {
    /**
     * HTTP Digest Md5 agent
     */
    private HttpDigestMd5Authentication mDigest;

    /**
     * Constructor
     */
    public ImsAkaRegistrationProcedure() {
    }

    /**
     * Initialize procedure
     */
    public void init() {
        mDigest = new HttpDigestMd5Authentication();
    }

    /**
     * Returns the home domain name
     * 
     * @return Domain name
     */
    public String getHomeDomain() {
        return ImsModule.getImsUserProfile().getHomeDomain();
    }

    /**
     * Returns the public URI or IMPU for registration
     * 
     * @return Public URI
     */
    public String getPublicUri() {
        UserProfile profile = ImsModule.getImsUserProfile();
        return new StringBuilder(PhoneUtils.SIP_URI_HEADER).append(profile.getUsername())
                .append("@").append(profile.getHomeDomain()).toString();
    }

    /**
     * Write security header to REGISTER request
     * 
     * @param request Request
     * @throws PayloadException
     */
    public void writeSecurityHeader(SipRequest request) throws PayloadException {
        try {
            String realm = mDigest.getRealm();
            UserProfile profile = ImsModule.getImsUserProfile();
            if (realm == null) {
                realm = profile.getRealm();
            }

            /* Each nonce value to be used only once */
            String nonce = "";
            if (mDigest.getNextnonce() != null) {
                mDigest.updateNonceParameters();
                nonce = mDigest.getNonce();
                mDigest.setNextnonce(null);
            }
            String requestUri = request.getRequestURI();
            String user = profile.getPrivateID();
            String response = "";
            byte[] auts = null;
            if (nonce.length() > 0) {
                String password;
                Map<SimChallengeUtils.Key, byte[]> authResult = SimChallengeUtils
                        .getIccSimChallengeResponse(nonce);
                byte[] res = authResult.get(SimChallengeUtils.Key.RES);
                if (res != null) {
                    password = new String(res, StringUtils.ISO_8859_1);
                } else {
                    auts = authResult.get(SimChallengeUtils.Key.AUTS);
                    if (auts != null) {
                        password = ""; // Sync failure, password is "" (see RFC3310)
                    } else {
                        // normally unreachable, else means sim challenge failed for wrong nonce
                        throw new PayloadException("Failed to get aka authentication response");
                    }
                }
                response = mDigest.calculateResponse(user, password, request.getMethod(),
                        requestUri, mDigest.buildNonceCounter(), request.getContent());
            }

            /* Build the Authorization header */
            StringBuilder auth = new StringBuilder("Digest username=\"").append(user)
                    .append("\",uri=\"").append(requestUri)
                    .append("\",algorithm=AKAv1-MD5,realm=\"").append(realm).append("\",nonce=\"")
                    .append(nonce).append("\",response=\"").append(response).append("\"");
            String opaque = mDigest.getOpaque();
            if (opaque != null) {
                auth.append(",opaque=\"").append(opaque).append("\"");
            }
            String qop = mDigest.getQop();
            if (qop != null && qop.startsWith("auth")) {
                auth.append(",nc=").append(mDigest.buildNonceCounter()).append(",qop=").append(qop)
                        .append(",cnonce=\"").append(mDigest.getCnonce()).append("\"");
            }
            if (auts != null) {
                // Synchronization Failure, append the AUTS to the Authorization header.
                auth.append(",auts=\"").append(Base64.encodeBase64ToString(auts)).append("\"");
            }
            request.addHeader(AuthorizationHeader.NAME, auth.toString());
        } catch (ParseException e) {
            throw new PayloadException("Failed to write security header!", e);
        }
    }

    /**
     * Read security header from REGISTER response
     * 
     * @param response SIP response
     * @throws PayloadException
     */
    public void readSecurityHeader(SipResponse response) throws PayloadException {
        WWWAuthenticateHeader wwwHeader = (WWWAuthenticateHeader) response
                .getHeader(WWWAuthenticateHeader.NAME);
        AuthenticationInfoHeader infoHeader = (AuthenticationInfoHeader) response
                .getHeader(AuthenticationInfoHeader.NAME);

        if (wwwHeader != null) {
            /* Retrieve data from the header WWW-Authenticate (401 response) */
            mDigest.setRealm(wwwHeader.getRealm());
            mDigest.setOpaque(wwwHeader.getOpaque());
            mDigest.setQop(wwwHeader.getQop());
            mDigest.setNextnonce(wwwHeader.getNonce());

        } else if (infoHeader != null) {
            // Retrieve data from the header Authentication-Info (200 OK response)

            // Check if 200 OK really included Authentication-Info: nextnonce=""
            if (infoHeader.getNextNonce() != null) {
                // Get nextnonce to be used
                mDigest.setNextnonce(infoHeader.getNextNonce());
            }
        }
    }

    /**
     * Returns HTTP digest
     * 
     * @return HTTP digest
     */
    public HttpDigestMd5Authentication getHttpDigest() {
        return mDigest;
    }
}
