/*
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2017 China Mobile.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.platform.AndroidFactory;
import com.gsma.rcs.utils.IntentUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.GroupChatIntent;
import com.gsma.services.rcs.chat.IGroupChatListener;
import com.gsma.services.rcs.chat.ChatLog.Message.Content;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.Participant.Status;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.ReasonCode;
import com.gsma.services.rcs.chat.ChatLog.GroupChat.State;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.content.Intent;
import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * GroupChatEventBroadcaster maintains the registering and unregistering of IGroupChatListener and
 * also performs broadcast events on these listeners upon the trigger of corresponding callbacks.
 */
public class GroupChatEventBroadcaster implements IGroupChatEventBroadcaster {

    private final RemoteCallbackList<IGroupChatListener> mGroupChatListeners = new RemoteCallbackList<>();

    private final Logger logger = Logger.getLogger(getClass().getName());

    public GroupChatEventBroadcaster() {
    }

    public void addGroupChatEventListener(IGroupChatListener listener) {
        mGroupChatListeners.register(listener);
    }

    public void removeGroupChatEventListener(IGroupChatListener listener) {
        mGroupChatListeners.unregister(listener);
    }

    @Override
    public void broadcastMessageStatusChanged(String chatId, String mimeType, String msgId,
                                              Content.Status status, Content.ReasonCode reasonCode) {
        int rcsStatus = status.toInt();
        int rcsReasonCode = reasonCode.toInt();
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onMessageStatusChanged(chatId, mimeType,
                        msgId, rcsStatus, rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastMessageGroupDeliveryInfoChanged(String chatId, ContactId contact,
            String apiMimeType, String msgId, GroupDeliveryInfo.Status status,
            GroupDeliveryInfo.ReasonCode reasonCode) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onMessageGroupDeliveryInfoChanged(chatId,
                        contact, apiMimeType, msgId, status.toInt(), reasonCode.toInt());
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }


    @Override
    public void broadcastSubjectChanged(String chatId, String subject) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onSubjectChanged(chatId, subject);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastOwnershipChanged(String chatId, ContactId contact) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onOwnershipChanged(chatId, contact);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastParticipantAliasChanged(String chatId, ContactId contact, String alias) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onParticipantAliasChanged(chatId, contact,
                        alias);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastParticipantStatusChanged(String chatId, ContactId contact,
            Status status) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onParticipantStatusChanged(chatId, contact,
                        status.toInt());
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastStateChanged(String chatId, State state, ReasonCode reasonCode) {
        final int N = mGroupChatListeners.beginBroadcast();
        int rcsState = state.toInt();
        int rcsReasonCode = reasonCode.toInt();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onStateChanged(chatId, rcsState,
                        rcsReasonCode);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastComposingEvent(String chatId, ContactId contact, boolean status) {
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onComposingEvent(chatId, contact, status);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastInvitation(String chatId) {
        Intent invitation = new Intent(GroupChatIntent.ACTION_NEW_INVITATION);
        invitation.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(invitation);
        invitation.putExtra(GroupChatIntent.EXTRA_CHAT_ID, chatId);
        AndroidFactory.getApplicationContext().sendBroadcast(invitation);
    }

    @Override
    public void broadcastMessageReceived(String apiMimeType, String msgId) {
        Intent newGroupChatMessage = new Intent(GroupChatIntent.ACTION_NEW_GROUP_CHAT_MESSAGE);
        newGroupChatMessage.addFlags(Intent.FLAG_EXCLUDE_STOPPED_PACKAGES);
        IntentUtils.tryToSetReceiverForegroundFlag(newGroupChatMessage);
        newGroupChatMessage.putExtra(GroupChatIntent.EXTRA_MIME_TYPE, apiMimeType);
        newGroupChatMessage.putExtra(GroupChatIntent.EXTRA_MESSAGE_ID, msgId);
        AndroidFactory.getApplicationContext().sendBroadcast(newGroupChatMessage);
    }

    @Override
    public void broadcastMessagesDeleted(String chatId, Set<String> msgIds) {
        List<String> msgIds2 = new ArrayList<>(msgIds);
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onMessagesDeleted(chatId, msgIds2);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }

    @Override
    public void broadcastGroupChatsDeleted(Set<String> chatIds) {
        List<String> ids = new ArrayList<>(chatIds);
        final int N = mGroupChatListeners.beginBroadcast();
        for (int i = 0; i < N; i++) {
            try {
                mGroupChatListeners.getBroadcastItem(i).onDeleted(ids);
            } catch (RemoteException e) {
                if (logger.isActivated()) {
                    logger.error("Can't notify listener.", e);
                }
            }
        }
        mGroupChatListeners.finishBroadcast();
    }
}
