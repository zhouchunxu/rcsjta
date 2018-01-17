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

package com.gsma.services.rcs.chat.sms;

import com.gsma.services.rcs.RcsGenericException;

/**
 * Standalone messaging service configuration
 */
public class StandaloneMessagingServiceConfiguration {

    private IStandaloneMessagingServiceConfiguration mIServiceConfig;

    /**
     * Constructor
     * 
     * @param serviceConfig
     * @hide
     */
    StandaloneMessagingServiceConfiguration(IStandaloneMessagingServiceConfiguration serviceConfig) {
        mIServiceConfig = serviceConfig;
    }

    /**
     * Returns the standalone message length limit. It returns 0 if there is no limitation.
     * 
     * @return long Size in bytes
     * @throws RcsGenericException
     */
    public long getStandaloneMessageMaxLength() throws RcsGenericException {
        try {
            return mIServiceConfig.getStandaloneMessageMaxLength();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Is standalone messaging supported
     * 
     * @return boolean Returns true if standalone messaging is supported else returns false
     * @throws RcsGenericException
     */
    public boolean isStandaloneMessagingSupported() throws RcsGenericException {
        try {
            return mIServiceConfig.isStandaloneMessagingSupported();

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }
}
