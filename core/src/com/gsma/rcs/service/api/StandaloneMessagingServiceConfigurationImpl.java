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

package com.gsma.rcs.service.api;

import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.sms.IStandaloneMessagingServiceConfiguration;

import android.os.RemoteException;

/**
 * A class that implements interface to allow access to standalone messaging service configuration
 * from API
 */
public class StandaloneMessagingServiceConfigurationImpl extends
        IStandaloneMessagingServiceConfiguration.Stub {

    private static final Logger sLogger = Logger
            .getLogger(StandaloneMessagingServiceConfigurationImpl.class.getSimpleName());

    private final RcsSettings mRcsSettings;

    /**
     * Constructor
     * 
     * @param rcsSettings RCS settings accessor
     */
    public StandaloneMessagingServiceConfigurationImpl(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    @Override
    public int getStandaloneMessageMaxLength() throws RemoteException {
        try {
            return mRcsSettings.getMaxStandaloneMsgLength();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }

    @Override
    public boolean isStandaloneMessagingSupported() throws RemoteException {
        try {
            return mRcsSettings.isStandaloneMessagingSupported();

        } catch (ServerApiBaseException e) {
            if (!e.shouldNotBeLogged()) {
                sLogger.error(ExceptionUtil.getFullStackTrace(e));
            }
            throw e;

        } catch (Exception e) {
            sLogger.error(ExceptionUtil.getFullStackTrace(e));
            throw new ServerApiGenericException(e);
        }
    }
}
