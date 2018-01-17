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

package com.gsma.rcs.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Sim authentication response data interface, see TS 102221 & 3GPP 31.102
 */
public class SimChallengeUtils {
    /**
     * Authentication response value types
     */
    public enum Key {
        RES, CK, IK, Kc, AUTS
    }

    /**
     * Successful 3G authentication; Successful VGCS/VBS operation authentication; Successful GBA
     * operation
     */
    private static final byte SUCCESSFUL = (byte) 0xDB;

    /**
     * 3G security context synchronisation failure; GBA security context (Bootstrapping Mode)
     * synchronisation failure
     */
    private static final byte SYNC_FAILURE = (byte) 0xDC;

    /**
     * Use the RAND and AUTN of the authentication vectors (RAND, XRES, CK, IK, AUTN) to generate
     * the APDU data parameter
     *
     * @param nonce Encoded (RAND || AUTN || server data...) of the authentication vectors
     * @return data Encoded command data (Lc) parameter of APDU
     */
    private static String createUmtsAuthenticationData(String nonce) {
        byte[] nonceBytes = Base64.decodeBase64(nonce.getBytes(StringUtils.UTF8));
        byte L1 = 16;
        byte L2 = 16;
        byte[] authData = new byte[L1 + L2 + 2];
        authData[0] = L1;// Length of RAND
        System.arraycopy(nonceBytes, 0, authData, 1, L1);// RAND
        authData[L1 + 1] = L2;// Length of AUTN
        System.arraycopy(nonceBytes, L1, authData, L1 + 2, L2);// AUTN
        return Base64.encodeBase64ToString(authData);
    }

    /**
     * Parse the response data in the 3G (UMTS) security context procedure
     *
     * @param data Icc APDU response data
     * @return result
     */
    private static Map<Key, byte[]> parseUmtsAuthenticationRespone(byte[] data) {
        Map<Key, byte[]> result = new HashMap<>();
        if (SUCCESSFUL == data[0]) {
            int L3 = data[1]; // Length of RES
            byte[] RES = new byte[L3];
            System.arraycopy(data, 2, RES, 0, L3);// RES
            result.put(Key.RES, RES);
            int L4 = data[L3 + 2];// Length of CK
            byte[] CK = new byte[L4];
            System.arraycopy(data, L3 + 3, CK, 0, L4);// CK
            result.put(Key.CK, CK);
            int L5 = data[L3 + L4 + 3];// Length of IK
            byte[] IK = new byte[L5];
            System.arraycopy(data, L3 + L4 + 4, IK, 0, L5);// IK
            result.put(Key.IK, IK);
            if (data.length > (L3 + L4 + L5 + 4)) {
                int L6 = data[L3 + L4 + L5 + 4];// Length of Kc = 8
                byte[] Kc = new byte[L6];
                System.arraycopy(data, L3 + L4 + L5 + 5, Kc, 0, L6);// Kc
                result.put(Key.Kc, Kc);
            }
        } else if (SYNC_FAILURE == data[0]) {
            int L1 = data[1];// Length of AUTS
            byte[] AUTS = new byte[L1];
            System.arraycopy(data, 2, AUTS, 0, L1);// AUTS
            result.put(Key.AUTS, AUTS);
        }
        return result;
    }

    public static Map<Key, byte[]> getIccSimChallengeResponse(String nonce) {
        /* 1st, construct the authentication challenge data */
        String data = createUmtsAuthenticationData(nonce);
        /* 2nd, get the authentication response data */
        String response = TelephonyUtils.getIccSimChallengeResponse(data);
        /* 3rd, parse the response */
        byte[] octects = Base64.decodeBase64(response.getBytes(StringUtils.UTF8));
        return parseUmtsAuthenticationRespone(octects);
    }
}
