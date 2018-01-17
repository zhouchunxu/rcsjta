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

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.Message.Content.Status;
import com.gsma.services.rcs.chat.sms.IOneToOneStandaloneMessagingListener;
import com.gsma.services.rcs.chat.sms.StandaloneMessagingIntent;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * OneToOneStandaloneMessagingEventBroadcaster maintains the registering and unregistering of
 * IOneToOneStandaloneMessagingListeners and also performs broadcast events on these listeners upon
 * the trigger of corresponding callbacks.
 */
public class OneToOneStandaloneMessagingEventBroadcaster implements
        IOneToOneStandaloneMessagingEventBroadcaster {

    private final RemoteCallbackList<IOneToOneStandaloneMessagingListener> mListeners = new RemoteCallbackList<>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public OneToOneStandaloneMessagingEventBroadcaster() {
    }

    public void addStandaloneMessagingEventListener(IOneToOneStandaloneMessagingListener listener) {
        mListeners.register(listener);
    }

    public void removeStandaloneMessagingEventListener(IOneToOneStandaloneMessagingListener listener) {
        mListeners.unregister(listener);
    }

    @Override
    public void broadcastMessageStatusChanged(String chatId, String mimeType, String msgId,
            Status status, ReasonCode reasonCode) {
        final int N = mListeners.beginBroadcast();
        int rcsStatus = status.toInt();
        int rcsReasonCode = reasonCode.toInt();
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onMessageStatusChanged(chatId, mimeType, msgId,
                        rcsStatus, rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }

    @Override
    public void broadcastMessageReceived(String mimeType, String msgId) {
        Intent newStandaloneMessage = new Intent(
                StandaloneMessagingIntent.ACTION_NEW_STANDALONE_MESSAGE);
        newStandaloneMessage.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(newStandaloneMessage);
        newStandaloneMessage.putExtra(StandaloneMessagingIntent.EXTRA_MIME_TYPE, mimeType);
        newStandaloneMessage.putExtra(StandaloneMessagingIntent.EXTRA_MESSAGE_ID, msgId);
        AndroidFactory.getApplicationContext().sendBroadcast(newStandaloneMessage);
    }

    @Override
    public void broadcastMessagesDeleted(String chatId, Set<String> msgIds) {
        List<String> ids = new ArrayList<>(msgIds);
        final int N = mListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mListeners.getBroadcastItem(i).onMessagesDeleted(chatId, ids);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mListeners.finishBroadcast();
    }
}
