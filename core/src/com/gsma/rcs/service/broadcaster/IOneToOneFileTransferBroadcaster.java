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

import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import java.util.Set;

/**
 * Interface to perform broadcast events on FileTransferListeners
 */
public interface IOneToOneFileTransferBroadcaster {

    void broadcastStateChanged(ContactId contact, String transferId, State status,
            ReasonCode reasonCode);

    void broadcastProgressUpdate(ContactId contact, String transferId, long currentSize,
            long totalSize);

    void broadcastInvitation(String fileTransferId);

    void broadcastFileTransfersDeleted(ContactId contact, Set<String> filetransferIds);
}
