/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2017 China Mobile.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.service.broadcaster;

import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.IOneToManyFileTransferListener;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;
import com.gsma.services.rcs.groupdelivery.GroupDeliveryInfo;

import android.os.RemoteCallbackList;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * OneToManyFileTransferBroadcaster maintains the registering and unregistering of
 * IOneToManyFileTransferListener and also performs broadcast events on these listeners upon the
 * trigger of corresponding callbacks.
 */
public class OneToManyFileTransferBroadcaster implements IOneToManyFileTransferBroadcaster {

    private final RemoteCallbackList<IOneToManyFileTransferListener> mOneToManyFileTransferListeners = new RemoteCallbackList<>();

    private final Logger logger = Logger
            .getLogger(OneToManyFileTransferBroadcaster.class.getName());

    public OneToManyFileTransferBroadcaster() {
    }

    public void addOneToManyFileTransferListener(IOneToManyFileTransferListener listener) {
        mOneToManyFileTransferListeners.register(listener);
    }

    public void removeOneToManyFileTransferListener(IOneToManyFileTransferListener listener) {
        mOneToManyFileTransferListeners.unregister(listener);
    }

    @Override
    public void broadcastStateChanged(String chatId, String transferId, State state,
            ReasonCode reasonCode) {
        synchronized (this) {
            final int N = mOneToManyFileTransferListeners.beginBroadcast();
            int rcsState = state.toInt();
            int rcsReasonCode = reasonCode.toInt();
            for (int i = 0; i < N; i++) {
                try {
                    mOneToManyFileTransferListeners.getBroadcastItem(i).onStateChanged(chatId,
                            transferId, rcsState, rcsReasonCode);
                } catch (RemoteException e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            mOneToManyFileTransferListeners.finishBroadcast();
        }
    }

    @Override
    public void broadcastProgressUpdate(String chatId, String transferId, long currentSize,
            long totalSize) {
        synchronized (this) {
            final int N = mOneToManyFileTransferListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    mOneToManyFileTransferListeners.getBroadcastItem(i).onProgressUpdate(chatId,
                            transferId, currentSize, totalSize);
                } catch (RemoteException e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener", e);
                    }
                }
            }
            mOneToManyFileTransferListeners.finishBroadcast();
        }
    }

    @Override
    public void broadcastDeliveryInfoChanged(String chatId, ContactId contact, String transferId,
            GroupDeliveryInfo.Status status, GroupDeliveryInfo.ReasonCode reasonCode) {
        synchronized (this) {
            int rcsStatus = status.toInt();
            int rcsReasonCode = reasonCode.toInt();
            final int N = mOneToManyFileTransferListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    mOneToManyFileTransferListeners.getBroadcastItem(i).onDeliveryInfoChanged(
                            chatId, contact, transferId, rcsStatus, rcsReasonCode);
                } catch (RemoteException e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener per contact", e);
                    }
                }
            }
            mOneToManyFileTransferListeners.finishBroadcast();
        }
    }

    @Override
    public void broadcastFileTransfersDeleted(String chatId, Set<String> transferIds) {
        synchronized (this) {
            List<String> ids = new ArrayList<>(transferIds);
            final int N = mOneToManyFileTransferListeners.beginBroadcast();
            for (int i = 0; i < N; i++) {
                try {
                    mOneToManyFileTransferListeners.getBroadcastItem(i).onDeleted(chatId, ids);
                } catch (RemoteException e) {
                    if (logger.isActivated()) {
                        logger.error("Can't notify listener per contact", e);
                    }
                }
            }
            mOneToManyFileTransferListeners.finishBroadcast();
        }
    }
}
