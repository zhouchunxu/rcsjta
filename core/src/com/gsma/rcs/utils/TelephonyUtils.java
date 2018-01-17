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

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.platform.telephony.TelephonyFactory;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettings;

import android.annotation.TargetApi;

@TargetApi(23)
public class TelephonyUtils {

    public static boolean isMultiSimEnabled() {
        return TelephonyFactory.getFactory().getPhoneCount() >= 2;
    }

    /**
     * Return the current account subId
     */
    public static int getCurrentSubId() {
        LocalContentResolver localResolver = new LocalContentResolver(AndroidFactory
                .getApplicationContext().getContentResolver());
        RcsSettings rcsSettings = RcsSettings.getInstance(localResolver);
        if (rcsSettings.isCmccRelease()) {
            return TelephonyFactory.getFactory().getDefaultDataSubId();
        }
        return TelephonyFactory.getFactory().getDefaultSubId();
    }

    /**
     * Returns the IMEI of the device
     */
    public static String getDeviceId() {
        if (isMultiSimEnabled()) {
            return TelephonyFactory.getFactory().getDeviceId(getCurrentSubId());
        } else {
            return TelephonyFactory.getFactory().getDeviceId();
        }
    }

    /**
     * Returns the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM. 5
     * or 6 decimal digits.
     */
    public static String getSimOperator() {
        if (isMultiSimEnabled()) {
            return TelephonyFactory.getFactory().getSimOperator(getCurrentSubId());
        } else {
            return TelephonyFactory.getFactory().getSimOperator();
        }
    }

    /**
     * Returns the Service Provider Name (SPN).
     */
    public static String getSimOperatorName() {
        if (isMultiSimEnabled()) {
            return TelephonyFactory.getFactory().getSimOperatorName(getCurrentSubId());
        } else {
            return TelephonyFactory.getFactory().getSimOperatorName();
        }
    }

    /**
     * Returns the unique subscriber ID (IMSI).
     */
    public static String getCurrentSubscriberId() {
        if (isMultiSimEnabled()) {
            return TelephonyFactory.getFactory().getSubscriberId(getCurrentSubId());
        } else {
            return TelephonyFactory.getFactory().getSubscriberId();
        }
    }

    /**
     * Returns the response of SIM Authentication.
     *
     * @param data authentication challenge data
     * @return the response of SIM Authentication, or null if not available
     */
    public static String getIccSimChallengeResponse(String data) {
        int appType = 2;// com.android.internal.telephony.PhoneConstants#APPTYPE_USIM = 2;
        if (isMultiSimEnabled()) {
            return TelephonyFactory.getFactory().getIccSimChallengeResponse(getCurrentSubId(),
                    appType, data);
        } else {
            return TelephonyFactory.getFactory().getIccSimChallengeResponse(appType, data);
        }
    }
}
