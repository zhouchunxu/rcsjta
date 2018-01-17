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

import com.gsma.rcs.platform.FactoryException;

/**
 * Application telephony factory
 */
public abstract class TelephonyFactory {
    /**
     * Current platform factory
     */
    private static TelephonyFactory mFactory;

    /**
     * Load the factory
     * 
     * @param classname Factory classname
     * @throws FactoryException
     */
    public static void loadFactory(String classname) throws FactoryException {
        if (mFactory != null) {
            return;
        }
        try {
            mFactory = (TelephonyFactory) Class.forName(classname).newInstance();
        } catch (InstantiationException e) {
            throw new FactoryException(new StringBuilder("Can't load the factory ").append(
                    classname).toString(), e);

        } catch (IllegalAccessException e) {
            throw new FactoryException(new StringBuilder("Can't load the factory ").append(
                    classname).toString(), e);

        } catch (ClassNotFoundException e) {
            throw new FactoryException(new StringBuilder("Can't load the factory ").append(
                    classname).toString(), e);
        }
    }

    /**
     * Returns the current factory
     * 
     * @return Factory
     */
    public static TelephonyFactory getFactory() {
        return mFactory;
    }

    /**
     * Returns the number of phones available.
     * <p>
     * Returns 1 for Single standby mode (Single SIM functionality), Returns 2 for Dual standby
     * mode.(Dual SIM functionality)
     */
    public abstract int getPhoneCount();

    /**
     * Returns the unique device ID, for example, the IMEI for GSM and the MEID or ESN for CDMA
     * phones. Return null if device ID is not available.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     */
    public abstract String getDeviceId();

    /**
     * Returns the unique device ID for a subscription, for example, the IMEI for GSM and the MEID
     * for CDMA phones. Return null if device ID is not available.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     *
     * @param slotId for which deviceID is returned
     */
    public abstract String getDeviceId(int slotId);

    /**
     * Returns the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM. 5
     * or 6 decimal digits.
     */
    public abstract String getSimOperator();

    /**
     * Returns the MCC+MNC (mobile country code + mobile network code) of the provider of the SIM
     * for a subscription. 5 or 6 decimal digits.
     *
     * @param subId for which SimOperator is returned
     * @hide
     */
    public abstract String getSimOperator(int subId);

    /**
     * Returns the Service Provider Name (SPN).
     */
    public abstract String getSimOperatorName();

    /**
     * Returns the Service Provider Name (SPN) for a subscription.
     *
     * @param subId for which SimOperatorName is returned
     * @hide
     */
    public abstract String getSimOperatorName(int subId);

    /**
     * Returns the unique subscriber ID, for example, the IMSI for a GSM phone. Return null if it is
     * unavailable.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     */
    public abstract String getSubscriberId();

    /**
     * Returns the unique subscriber ID, for example, the IMSI for a GSM phone for a subscription.
     * Return null if it is unavailable.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     *
     * @param subId whose subscriber id is returned
     * @hide
     */
    public abstract String getSubscriberId(int subId);

    /**
     * Returns the MSISDN string. for a GSM phone. Return null if it is unavailable.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     */
    public abstract String getMsisdn();

    /**
     * Returns the MSISDN string. for a GSM phone. Return null if it is unavailable.
     * <p>
     * Requires Permission: {@link android.Manifest.permission#READ_PHONE_STATE READ_PHONE_STATE}
     *
     * @param subId for which msisdn is returned
     * @hide
     */
    public abstract String getMsisdn(int subId);

    /**
     * Returns the response of SIM Authentication through RIL for the default subscription. Returns
     * null if the Authentication hasn't been successful
     * 
     * @param appType ICC application type (@see
     *            com.android.internal.telephony.PhoneConstants#APPTYPE_xxx)
     * @param data authentication challenge data
     * @return the response of SIM Authentication, or null if not available
     */
    public abstract String getIccSimChallengeResponse(int appType, String data);

    /**
     * Returns the response of SIM Authentication through RIL. Returns null if the Authentication
     * hasn't been successful
     * 
     * @param subId subscription ID to be queried
     * @param appType ICC application type (@see
     *            com.android.internal.telephony.PhoneConstants#APPTYPE_xxx)
     * @param data authentication challenge data
     * @return the response of SIM Authentication, or null if not available
     * @hide
     */
    public abstract String getIccSimChallengeResponse(int subId, int appType, String data);

    /**
     * Get slotId associated with the subscription.
     * 
     * @return slotId as a positive integer or a negative value if an error either SIM_NOT_INSERTED
     *         or < 0 if an invalid slot index
     */
    public abstract int getSlotId(int subId);

    /**
     * Get the subId associated with the slot.
     * 
     * @return subId of the default subscription or a value < 0 if an error.
     */
    public abstract int getSubId(int slotId);

    /**
     * @return the "system" defaultSubId on a voice capable device this will be
     *         getDefaultVoiceSubId() and on a data only device it will be getDefaultDataSubId().
     * @hide
     */
    public abstract int getDefaultSubId();

    /**
     * @return the "system" default voice subId
     * @hide
     */
    public abstract int getDefaultVoiceSubId();

    /**
     * @return the "system" default data subId
     * @hide
     */
    public abstract int getDefaultDataSubId();

}
