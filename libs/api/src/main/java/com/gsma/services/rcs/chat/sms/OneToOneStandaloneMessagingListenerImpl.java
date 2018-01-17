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

import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.contact.ContactId;

import android.os.RemoteException;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * One to one standalone messaging event listener implementation
 */
public class OneToOneStandaloneMessagingListenerImpl extends
        IOneToOneStandaloneMessagingListener.Stub {

    private final OneToOneStandaloneMessagingListener mListener;

    private final static String LOG_TAG = OneToOneStandaloneMessagingListenerImpl.class.getName();

    OneToOneStandaloneMessagingListenerImpl(OneToOneStandaloneMessagingListener listener) {
        mListener = listener;
    }

    @Override
    public void onMessageStatusChanged(String chatId, String mimeType, String msgId, int status,
            int reasonCode) throws RemoteException {
        Status rcsStatus;
        ReasonCode rcsReasonCode;
        try {
            rcsStatus = Status.valueOf(status);
            rcsReasonCode = ReasonCode.valueOf(reasonCode);
        } catch (IllegalArgumentException e) {
            /*
             * Detected unknown state or reasonCode not part of standard coming from stack which a
             * client application can of course not handle since it is build only to handle the
             * possible enum values documented and specified in the api standard.
             */
            Log.e(LOG_TAG, e.getMessage());
            return;
        }
        mListener.onMessageStatusChanged(chatId, mimeType, msgId, rcsStatus, rcsReasonCode);
    }

    @Override
    public void onMessagesDeleted(String chatId, List<String> msgIds) throws RemoteException {
        mListener.onMessagesDeleted(chatId, new HashSet<>(msgIds));
    }

}
