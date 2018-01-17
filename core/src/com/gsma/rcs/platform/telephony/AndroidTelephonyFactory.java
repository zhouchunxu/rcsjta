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

package com.gsma.rcs.platform.telephony;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.logger.Logger;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import java.lang.reflect.Method;

/**
 * Android telephony factory
 */
@TargetApi(Build.VERSION_CODES.M)
public class AndroidTelephonyFactory extends TelephonyFactory {

    /**
     * An invalid subscription identifier
     */
    public static final int INVALID_SUBSCRIPTION_ID = -1;

    /**
     * An invalid slot identifier
     */
    public static final int INVALID_SIM_SLOT_INDEX = -1;

    private final TelephonyManager mTelephonyManager;

    private final SubscriptionManager mSubscriptionManager;

    /**
     * Logger
     */
    private static final Logger sLogger = Logger.getLogger(AndroidTelephonyFactory.class
            .getSimpleName());

    /**
     * Constructor
     */
    public AndroidTelephonyFactory() {
        Context context = AndroidFactory.getApplicationContext();
        mTelephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mSubscriptionManager = (SubscriptionManager) context
                .getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    @Override
    public int getPhoneCount() {
        return mTelephonyManager.getPhoneCount();
    }

    @Override
    public String getDeviceId() {
        return mTelephonyManager.getDeviceId();
    }

    @Override
    public String getDeviceId(int slotId) {
        return mTelephonyManager.getDeviceId(slotId);
    }

    @Override
    public String getSimOperator() {
        return mTelephonyManager.getSimOperator();
    }

    @Override
    public String getSimOperator(int subId) {
        String operator = "";
        try {
            Method m = mTelephonyManager.getClass().getMethod("getSimOperator", int.class);
            operator = (String) m.invoke(mTelephonyManager, subId);
        } catch (Exception e) {
            sLogger.error("Failed to get the sim operator of sub " + subId, e);
        }
        return operator;
    }

    @Override
    public String getSimOperatorName() {
        return mTelephonyManager.getSimOperatorName();
    }

    @Override
    public String getSimOperatorName(int subId) {
        String operatorName = "";
        try {
            Method m = mTelephonyManager.getClass().getMethod("getSimOperatorName", int.class);
            operatorName = (String) m.invoke(mTelephonyManager, subId);
        } catch (Exception e) {
            sLogger.error("Failed to get the sim operator name of sub " + subId, e);
        }
        return operatorName;
    }

    @Override
    public String getSubscriberId() {
        return mTelephonyManager.getSubscriberId();
    }

    @Override
    public String getSubscriberId(int subId) {
        String subscriberId = "";
        try {
            Method m = mTelephonyManager.getClass().getMethod("getSubscriberId", int.class);
            subscriberId = (String) m.invoke(mTelephonyManager, subId);
        } catch (Exception e) {
            sLogger.error("Failed to get the subscriber (IMSI) of sub " + subId, e);
        }
        return subscriberId;
    }

    @Override
    public String getMsisdn() {
        String msisdn = "";
        try {
            Method m = mTelephonyManager.getClass().getMethod("getMsisdn");
            msisdn = (String) m.invoke(mTelephonyManager);
        } catch (Exception e) {
            sLogger.error("Failed to get the msisdn", e);
        }
        return msisdn;
    }

    @Override
    public String getMsisdn(int subId) {
        String msisdn = "";
        try {
            Method m = mTelephonyManager.getClass().getMethod("getMsisdn", int.class);
            msisdn = (String) m.invoke(mTelephonyManager, subId);
        } catch (Exception e) {
            sLogger.error("Failed to get the msisdn of sub " + subId, e);
        }
        return msisdn;
    }

    @Override
    public String getIccSimChallengeResponse(int appType, String data) {
        String response = "";
        try {
            Method m = mTelephonyManager.getClass().getDeclaredMethod("getIccSimChallengeResponse",
                    int.class, String.class);
            response = (String) m.invoke(mTelephonyManager, appType, data);
        } catch (Exception e) {
            sLogger.error("Failed to get the sim challenge response", e);
        }
        return response;
    }

    @Override
    public String getIccSimChallengeResponse(int subId, int appType, String data) {
        String response = "";
        try {
            Method m = mTelephonyManager.getClass().getDeclaredMethod("getIccSimChallengeResponse",
                    int.class, int.class, String.class);
            response = (String) m.invoke(mTelephonyManager, subId, appType, data);
        } catch (Exception e) {
            sLogger.error("Failed to get the sim challenge response of sub " + subId, e);
        }
        return response;
    }

    @Override
    public int getSlotId(int subId) {
        int slotId = INVALID_SIM_SLOT_INDEX;
        try {
            Method m = mSubscriptionManager.getClass().getDeclaredMethod("getSlotId");
            slotId = (int) m.invoke(mSubscriptionManager);
        } catch (Exception e) {
            sLogger.error("Failed to get the slot id of sub " + subId, e);
        }
        return slotId;
    }

    @Override
    public int getSubId(int slotId) {
        int subId = INVALID_SUBSCRIPTION_ID;
        try {
            Method m = mSubscriptionManager.getClass().getDeclaredMethod("getSubId");
            subId = (int) m.invoke(mSubscriptionManager);
        } catch (Exception e) {
            sLogger.error("Failed to get the subId of slot " + slotId, e);
        }
        return subId;
    }

    @Override
    public int getDefaultSubId() {
        int subId = INVALID_SUBSCRIPTION_ID;
        try {
            Method m = mSubscriptionManager.getClass().getDeclaredMethod("getDefaultSubId");
            subId = (int) m.invoke(mSubscriptionManager);
        } catch (Exception e) {
            sLogger.error("Failed to get the default subId", e);
        }
        return subId;
    }

    @Override
    public int getDefaultVoiceSubId() {
        int subId = INVALID_SUBSCRIPTION_ID;
        try {
            Method m = mSubscriptionManager.getClass().getDeclaredMethod("getDefaultVoiceSubId");
            subId = (int) m.invoke(mSubscriptionManager);
        } catch (Exception e) {
            sLogger.error("Failed to get the default voice subId", e);
        }
        return subId;
    }

    @Override
    public int getDefaultDataSubId() {
        int subId = INVALID_SUBSCRIPTION_ID;
        try {
            Method m = mSubscriptionManager.getClass().getDeclaredMethod("getDefaultDataSubId");
            subId = (int) m.invoke(mSubscriptionManager);
        } catch (Exception e) {
            sLogger.error("Failed to get the default data subId", e);
        }
        return subId;
    }
}
