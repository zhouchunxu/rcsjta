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
import com.gsma.services.rcs.RcsIllegalArgumentException;
import com.gsma.services.rcs.RcsPermissionDeniedException;
import com.gsma.services.rcs.RcsPersistentStorageException;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.RcsServiceControl;
import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceListener;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.RcsServiceNotRegisteredException;
import com.gsma.services.rcs.RcsServiceListener.ReasonCode;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.contact.ContactId;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * This class offers the main entry point to send standalone messages. Several applications may
 * connect/disconnect to the API. The parameter contact in the API supports the following formats:
 * MSISDN in national or international format, SIP address, SIP-URI or Tel-URI.
 */
public final class StandaloneMessagingService extends RcsService {
    /**
     * API
     */
    private IStandaloneMessagingService mApi;

    private final Map<OneToOneStandaloneMessagingListener, WeakReference<IOneToOneStandaloneMessagingListener>> mOneToOneStandaloneMessagingListeners = new WeakHashMap<>();

    private final Map<OneToManyStandaloneMessagingListener, WeakReference<IOneToManyStandaloneMessagingListener>> mOneToManyStandaloneMessagingListeners = new WeakHashMap<>();

    private static boolean sApiCompatible = false;

    /**
     * Constructor
     * 
     * @param ctx Application context
     * @param listener Service listener
     */
    public StandaloneMessagingService(Context ctx, RcsServiceListener listener) {
        super(ctx, listener);
    }

    /**
     * Connects to the API
     * 
     * @throws RcsPermissionDeniedException
     */
    public final void connect() throws RcsPermissionDeniedException {
        if (!sApiCompatible) {
            try {
                sApiCompatible = mRcsServiceControl.isCompatible(this);
                if (!sApiCompatible) {
                    throw new RcsPermissionDeniedException(
                            "The TAPI client version of the standalone messaging service is not compatible with the TAPI service implementation version on this device!");
                }
            } catch (RcsServiceException e) {
                throw new RcsPermissionDeniedException(
                        "The compatibility of TAPI client version with the TAPI service implementation version of this device cannot be checked for the standalone messaging service!",
                        e);
            }
        }
        Intent serviceIntent = new Intent(IStandaloneMessagingService.class.getName());
        serviceIntent.setPackage(RcsServiceControl.RCS_STACK_PACKAGENAME);
        mCtx.bindService(serviceIntent, apiConnection, 0);
    }

    /**
     * Disconnects from the API
     */
    public void disconnect() {
        try {
            mCtx.unbindService(apiConnection);
        } catch (IllegalArgumentException e) {
            // Nothing to do
        }
    }

    /**
     * Set API interface
     * 
     * @param api API interface
     */
    protected void setApi(IInterface api) {
        super.setApi(api);
        mApi = (IStandaloneMessagingService) api;
    }

    /**
     * Service connection
     */
    private ServiceConnection apiConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            setApi(IStandaloneMessagingService.Stub.asInterface(service));
            if (mListener != null) {
                mListener.onServiceConnected();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            setApi(null);
            if (mListener == null) {
                return;
            }
            ReasonCode reasonCode = ReasonCode.CONNECTION_LOST;
            try {
                if (!mRcsServiceControl.isActivated()) {
                    reasonCode = ReasonCode.SERVICE_DISABLED;
                }
            } catch (RcsServiceException e) {
                // Do nothing
            }
            mListener.onServiceDisconnected(reasonCode);
        }
    };

    /**
     * Returns the configuration of the standalone messaging service
     * 
     * @return StandaloneMessagingServiceConfiguration
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public StandaloneMessagingServiceConfiguration getConfiguration()
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new StandaloneMessagingServiceConfiguration(mApi.getConfiguration());

        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to send a standalone message to the contact specified by the
     * contact parameter, else returns false.
     * 
     * @param contact Contact
     * @return boolean
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public boolean isAllowedToSendMessage(ContactId contact)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.isAllowedToSendMessage(contact);

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns true if it is possible to send a many standalone message to the recipients specified
     * by the contacts parameter, else returns false.
     * 
     * @param contacts Contacts
     * @return boolean
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public boolean isAllowedToSendMessageToMany(Set<ContactId> contacts)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return mApi.isAllowedToSendMessageToMany(new ArrayList<>(contacts));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Send a standalone message to a contact. The parameter contact supports the following formats:
     * MSISDN in national or international format, SIP address, SIP-URI or Tel-URI. If the format of
     * the contact is not supported an exception is thrown.
     * 
     * @param contact Contact Identifier
     * @param message Message
     * @return StandaloneMessage
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     * @throws RcsPermissionDeniedException
     * @throws RcsServiceNotRegisteredException
     */
    public ChatMessage sendMessage(ContactId contact, String message)
            throws RcsPersistentStorageException, RcsServiceNotAvailableException,
            RcsGenericException, RcsPermissionDeniedException, RcsServiceNotRegisteredException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ChatMessage(mApi.sendMessage(contact, message));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Send a standalone message to many.
     * 
     * @param contacts Contacts
     * @param message Message
     * @return StandaloneMessage
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     * @throws RcsPermissionDeniedException
     * @throws RcsServiceNotRegisteredException
     */
    public ChatMessage sendMessageToMany(Set<ContactId> contacts, String message)
            throws RcsPersistentStorageException, RcsServiceNotAvailableException,
            RcsGenericException, RcsPermissionDeniedException, RcsServiceNotRegisteredException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ChatMessage(mApi.sendMessageToMany(new ArrayList<>(contacts), message));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPermissionDeniedException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            RcsServiceNotRegisteredException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Resend a message which previously failed.
     * 
     * @param msgId
     * @throws RcsPersistentStorageException
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void resendMessage(String msgId) throws RcsPersistentStorageException,
            RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.resendMessage(msgId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Mark a received message as read (ie. displayed in the UI)
     * 
     * @param msgId Message id
     * @throws RcsServiceNotAvailableException
     * @throws RcsPersistentStorageException
     * @throws RcsGenericException
     */
    public void markMessageAsRead(String msgId) throws RcsServiceNotAvailableException,
            RcsPersistentStorageException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.markMessageAsRead(msgId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            RcsPersistentStorageException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on one to one standalone messaging events
     * 
     * @param listener One to one standalone messaging listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(OneToOneStandaloneMessagingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IOneToOneStandaloneMessagingListener rcsListener = new OneToOneStandaloneMessagingListenerImpl(
                    listener);
            mOneToOneStandaloneMessagingListeners.put(listener, new WeakReference<>(rcsListener));
            mApi.addEventListener2(rcsListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on one to one standalone messaging events
     * 
     * @param listener One to one standalone messaging listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(OneToOneStandaloneMessagingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IOneToOneStandaloneMessagingListener> weakRef = mOneToOneStandaloneMessagingListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IOneToOneStandaloneMessagingListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener2(rcsListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Adds a listener on one to many standalone messaging events
     * 
     * @param listener One to many standalone messaging listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void addEventListener(OneToManyStandaloneMessagingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (listener == null) {
            throw new RcsIllegalArgumentException("listener must not be null!");
        }
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            IOneToManyStandaloneMessagingListener rcsListener = new OneToManyStandaloneMessagingListenerImpl(
                    listener);
            mOneToManyStandaloneMessagingListeners.put(listener, new WeakReference<>(rcsListener));
            mApi.addEventListener3(rcsListener);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Removes a listener on one to many standalone messaging events
     * 
     * @param listener One to many standalone messaging listener
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void removeEventListener(OneToManyStandaloneMessagingListener listener)
            throws RcsServiceNotAvailableException, RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            WeakReference<IOneToManyStandaloneMessagingListener> weakRef = mOneToManyStandaloneMessagingListeners
                    .remove(listener);
            if (weakRef == null) {
                return;
            }
            IOneToManyStandaloneMessagingListener rcsListener = weakRef.get();
            if (rcsListener != null) {
                mApi.removeEventListener3(rcsListener);
            }
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Returns the standalone message from its unique ID
     * 
     * @param msgId Message id
     * @return StandaloneMessage
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public ChatMessage getMessage(String msgId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            return new ChatMessage(mApi.getMessage(msgId));

        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes one to one standalone messages from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteOneToOneStandaloneMessages() throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteOneToOneStandaloneMessages();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes one to many standalone messages from history and abort/reject any associated ongoing
     * session if such exists.
     * 
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteOneToManyStandaloneMessages() throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteOneToManyStandaloneMessages();
        } catch (Exception e) {
            throw new RcsGenericException(e);
        }
    }

    /**
     * Deletes one to one standalone messages with a given contact from history and abort/reject any
     * associated ongoing session if such exists.
     * 
     * @param contact Contact
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteOneToOneStandaloneMessages(ContactId contact) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteOneToOneStandaloneMessages2(contact);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Delete all one to many standalone messages with the given contacts from history and
     * abort/reject any associated ongoing session if such exists.
     * 
     * @param contacts Contacts Id
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteOneToManyStandaloneMessages(Set<ContactId> contacts) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteOneToManyStandaloneMessages2(new ArrayList<>(contacts));
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }

    /**
     * Delete the standalone message from its unique message id from history.
     * 
     * @param msgId Message id
     * @throws RcsServiceNotAvailableException
     * @throws RcsGenericException
     */
    public void deleteMessage(String msgId) throws RcsServiceNotAvailableException,
            RcsGenericException {
        if (mApi == null) {
            throw new RcsServiceNotAvailableException();
        }
        try {
            mApi.deleteMessage(msgId);
        } catch (Exception e) {
            RcsIllegalArgumentException.assertException(e);
            throw new RcsGenericException(e);
        }
    }
}
