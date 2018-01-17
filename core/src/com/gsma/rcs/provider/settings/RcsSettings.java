/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 * Copyright (C) 2017 China Mobile.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p/>
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.provider.settings;

import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.capability.Capabilities.CapabilitiesBuilder;
import com.gsma.rcs.core.ims.service.extension.ServiceExtensionManager;
import com.gsma.rcs.core.ims.service.sip.EnrichCallingService;
import com.gsma.rcs.provider.CursorUtil;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.gsma.rcs.provider.settings.RcsSettingsData.EnableRcseSwitch;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImMsgTech;
import com.gsma.rcs.provider.settings.RcsSettingsData.ImSessionStartMode;
import com.gsma.rcs.provider.settings.RcsSettingsData.NetworkAccessType;
import com.gsma.rcs.provider.settings.RcsSettingsData.TermsAndConditionsResponse;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.CommonServiceConfiguration.MinimumBatteryLevel;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration.ImageResizeOption;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * RCS settings
 *
 * @author jexa7410
 * @author Philippe LEMORDANT
 */
public class RcsSettings {

    private final static Logger sLogger = Logger.getLogger(RcsSettings.class.getName());

    /**
     * Minimum number of participants in a group chat
     */
    private static final int MINIMUM_GC_PARTICIPANTS = 2;

    /**
     * The maximum length of the Group Chat subject
     */
    private static final int GROUP_CHAT_SUBJECT_MAX_LENGTH = 50;

    /**
     * The maximum length of the alias displayed in Group Chat
     */
    private static final int GROUP_CHAT_ALIAS_MAX_LENGTH = 30;

    private static final String WHERE_CLAUSE = RcsSettingsData.KEY_KEY + "=?";

    /**
     * Current instance
     */
    private static volatile RcsSettings sInstance;

    /**
     * Local Content resolver
     */
    final private LocalContentResolver mLocalContentResolver;

    /**
     * A cache for storing settings in order to increase performance
     */
    final private Map<String, Object> mCache;

    /**
     * Get or Create Singleton instance of RcsSettings.
     *
     * @param localContentResolver Local content resolver
     * @return RcsSettings instance
     */
    public static RcsSettings getInstance(LocalContentResolver localContentResolver) {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (RcsSettings.class) {
            if (sInstance == null) {
                sInstance = new RcsSettings(localContentResolver);
            }
            return sInstance;
        }
    }

    /**
     * Constructor
     *
     * @param localContentResolver Local content resolver
     */
    private RcsSettings(LocalContentResolver localContentResolver) {
        super();
        mLocalContentResolver = localContentResolver;
        mCache = new HashMap<>();
    }

    private ContentProviderOperation buildContentProviderOp(String key, String value) {
        return ContentProviderOperation.newUpdate(RcsSettingsData.CONTENT_URI)
                .withValue(RcsSettingsData.KEY_VALUE, value)
                .withSelection(WHERE_CLAUSE, new String[] {
                    key
                }).build();
    }

    private ContentProviderOperation buildContentProviderOp(String key, Object value) {
        return buildContentProviderOp(key, value == null ? null : value.toString());
    }

    /**
     * Read boolean parameter
     * <p/>
     * If parsing of the value fails, method return false.
     *
     * @param key the key field
     * @return the value field
     */
    public boolean readBoolean(String key) {
        Boolean value = (Boolean) mCache.get(key);
        if (value == null) {
            value = Boolean.parseBoolean(readParameter(key));
            mCache.put(key, value);
        }
        return value;
    }

    /**
     * Write boolean parameter
     *
     * @param key the key field
     * @param value the boolean value
     */
    public void writeBoolean(String key, Boolean value) {
        if (writeParameter(key, value.toString()) != 0) {
            mCache.put(key, value);
        }
    }

    /**
     * Read int parameter
     * <p/>
     * If parsing of the value fails, method return default value.
     *
     * @param key the key field
     * @return the value field
     */
    public int readInteger(String key) {
        Integer value = (Integer) mCache.get(key);
        if (value == null) {
            value = Integer.parseInt(readParameter(key));
            mCache.put(key, value);
        }
        return value;
    }

    /**
     * Read long parameter
     * <p/>
     * If parsing of the value fails, method return default value.
     *
     * @param key the key field
     * @return the value field
     */
    public long readLong(String key) {
        Long value = (Long) mCache.get(key);
        if (value == null) {
            value = Long.parseLong(readParameter(key));
            mCache.put(key, value);
        }
        return value;
    }

    /**
     * Read String parameter
     *
     * @param key the key field
     * @return the value field or defaultValue (if read fails)
     */
    public String readString(String key) {
        String value = (String) mCache.get(key);
        if (value == null && !mCache.containsKey(key)) {
            value = readParameter(key);
            mCache.put(key, value);
        }
        return value;
    }

    /**
     * Write integer parameter
     *
     * @param key the key field
     * @param value the integer value
     */
    public void writeInteger(String key, Integer value) {
        if (writeParameter(key, value.toString()) != 0) {
            mCache.put(key, value);
        }
    }

    /**
     * Write long parameter
     *
     * @param key the key field
     * @param value the long value
     */
    public void writeLong(String key, Long value) {
        if (writeParameter(key, value.toString()) != 0) {
            mCache.put(key, value);
        }
    }

    /**
     * Write String parameter
     *
     * @param key the key field
     * @param value the long value
     */
    public void writeString(String key, String value) {
        if (writeParameter(key, value) != 0) {
            mCache.put(key, value);
        }
    }

    /**
     * Read Uri parameter
     *
     * @param key the key field
     * @return the value field or defaultValue (if read fails)
     */
    public Uri readUri(String key) {
        Uri value = (Uri) mCache.get(key);
        if (value == null && !mCache.containsKey(key)) {
            String dbValue = readParameter(key);
            if (dbValue != null) {
                value = Uri.parse(dbValue);
            }
            mCache.put(key, value);
        }
        return value;
    }

    /**
     * Write uri parameter
     *
     * @param key the key field
     * @param value the long value
     */
    public void writeUri(String key, Uri value) {
        if (writeParameter(key, value == null ? null : value.toString()) != 0) {
            mCache.put(key, value);
        }
    }

    /**
     * Read ContactId parameter
     *
     * @param key the key field
     * @return the value field or defaultValue (if read fails)
     */
    public ContactId readContactId(String key) {
        ContactId value = (ContactId) mCache.get(key);
        if (value == null && !mCache.containsKey(key)) {
            String dbValue = readParameter(key);
            if (dbValue != null) {
                value = ContactUtil.createContactIdFromTrustedData(dbValue);
            }
            mCache.put(key, value);
        }
        return value;
    }

    /**
     * Write ContactId parameter
     *
     * @param key the key field
     * @param value the long value
     */
    public void writeContactId(String key, ContactId value) {
        if (writeParameter(key, value == null ? null : value.toString()) != 0) {
            mCache.put(key, value);
        }
    }

    /**
     * Read a parameter from database
     *
     * @param key Key
     * @return Value
     */
    private String readParameter(String key) {
        Cursor c = null;
        try {
            String[] whereArg = new String[] {
                key
            };
            c = mLocalContentResolver.query(RcsSettingsData.CONTENT_URI, null, WHERE_CLAUSE,
                    whereArg, null);
            CursorUtil.assertCursorIsNotNull(c, RcsSettingsData.CONTENT_URI);
            if (!c.moveToFirst()) {
                throw new IllegalArgumentException("Illegal setting key:" + key);
            }
            return c.getString(c.getColumnIndexOrThrow(RcsSettingsData.KEY_VALUE));

        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    /**
     * Write a string setting parameter to Database
     *
     * @param key the key setting
     * @param value the value setting
     * @return the number of rows updated
     */
    private int writeParameter(String key, String value) {
        ContentValues values = new ContentValues();
        values.put(RcsSettingsData.KEY_VALUE, value);
        String[] whereArgs = new String[] {
            key
        };
        return mLocalContentResolver.update(RcsSettingsData.CONTENT_URI, values, WHERE_CLAUSE,
                whereArgs);
    }

    /**
     * Is RCS service activated
     *
     * @return Boolean
     */
    public boolean isServiceActivated() {
        return readBoolean(RcsSettingsData.SERVICE_ACTIVATED);
    }

    /**
     * Set the RCS service activation state
     *
     * @param state State
     */
    public void setServiceActivationState(boolean state) {
        writeBoolean(RcsSettingsData.SERVICE_ACTIVATED, state);
    }

    /**
     * Is send displayed notification activated
     *
     * @return Boolean
     */
    public boolean isRespondToDisplayReports() {
        return readBoolean(RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS);
    }

    /**
     * Set send displayed notification
     *
     * @param state True if respond to display reports
     */
    public void setRespondToDisplayReports(boolean state) {
        writeBoolean(RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS, state);
    }

    /**
     * Get the min battery level
     *
     * @return Battery level in percentage
     */
    public MinimumBatteryLevel getMinBatteryLevel() {
        return MinimumBatteryLevel.valueOf(readInteger(RcsSettingsData.MIN_BATTERY_LEVEL));
    }

    /**
     * Set the min battery level
     *
     * @param level Battery level in percentage
     */
    public void setMinBatteryLevel(MinimumBatteryLevel level) {
        writeInteger(RcsSettingsData.MIN_BATTERY_LEVEL, level.toInt());
    }

    /**
     * Get user profile username (i.e. username part of the IMPU)
     *
     * @return Username part of SIP-URI or null if not provisioned
     */
    public ContactId getUserProfileImsUserName() {
        return readContactId(RcsSettingsData.USERPROFILE_IMS_USERNAME);
    }

    /**
     * Set user profile IMS username (i.e. username part of the IMPU)
     *
     * @param contact the contact ID
     */
    public void setUserProfileImsUserName(ContactId contact) {
        writeContactId(RcsSettingsData.USERPROFILE_IMS_USERNAME, contact);
    }

    /**
     * Get UUID(Universally Unique Identifier) format: 8-4-4-4-12 hex digits
     *
     * @return uuid value
     */
    public String getUUID() {
        return readString(RcsSettingsData.UUID);
    }

    /**
     * Get user profile IMS display name associated to IMPU
     *
     * @return String
     */
    public String getUserProfileImsDisplayName() {
        return readString(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME);
    }

    /**
     * Set user profile IMS display name associated to IMPU
     *
     * @param value Value
     */
    public void setUserProfileImsDisplayName(String value) {
        writeString(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, value);
    }

    /**
     * Get user profile IMS public Id of PC (i.e. IMPU of PC)
     *
     * @return SIP-URI
     */
    public String getUserProfileImsPublicIdPc() {
        return readString(RcsSettingsData.USERPROFILE_IMS_PUBLIC_ID_PC);
    }

    /**
     * Set user profile IMS public Id of PC (i.e. IMPU of PC)
     *
     * @param uri SIP-URI
     */
    public void setUserProfileImsPublicIdPc(String uri) {
        writeString(RcsSettingsData.USERPROFILE_IMS_PUBLIC_ID_PC, uri);
    }

    /**
     * Get user profile IMS private Id (i.e. IMPI)
     *
     * @return SIP-URI
     */
    public String getUserProfileImsPrivateId() {
        return readString(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID);
    }

    /**
     * Set user profile IMS private Id (i.e. IMPI)
     *
     * @param uri SIP-URI
     */
    public void setUserProfileImsPrivateId(String uri) {
        writeString(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, uri);
    }

    /**
     * Get user profile IMS password
     *
     * @return String
     */
    public String getUserProfileImsPassword() {
        return readString(RcsSettingsData.USERPROFILE_IMS_PASSWORD);
    }

    /**
     * Set user profile IMS password
     *
     * @param pwd Password
     */
    public void setUserProfileImsPassword(String pwd) {
        writeString(RcsSettingsData.USERPROFILE_IMS_PASSWORD, pwd);
    }

    /**
     * Get user profile IMS realm
     *
     * @return String
     */
    public String getUserProfileImsRealm() {
        return readString(RcsSettingsData.USERPROFILE_IMS_REALM);
    }

    /**
     * Set user profile IMS realm
     *
     * @param realm Realm
     */
    public void setUserProfileImsRealm(String realm) {
        writeString(RcsSettingsData.USERPROFILE_IMS_REALM, realm);
    }

    /**
     * Get user profile IMS home domain
     *
     * @return Domain
     */
    public String getUserProfileImsDomain() {
        return readString(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN);
    }

    /**
     * Set user profile IMS home domain
     *
     * @param domain Domain
     */
    public void setUserProfileImsDomain(String domain) {
        writeString(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, domain);
    }

    /**
     * Get IMS proxy address for mobile access
     *
     * @return Address
     */
    public String getImsProxyAddrForMobile() {
        return readString(RcsSettingsData.IMS_PROXY_ADDR_MOBILE);
    }

    /**
     * Set IMS proxy address for mobile access
     *
     * @param addr Address
     */
    public void setImsProxyAddrForMobile(String addr) {
        writeString(RcsSettingsData.IMS_PROXY_ADDR_MOBILE, addr);
    }

    /**
     * Get IMS proxy port for mobile access
     *
     * @return Port
     */
    public int getImsProxyPortForMobile() {
        return readInteger(RcsSettingsData.IMS_PROXY_PORT_MOBILE);
    }

    /**
     * Set IMS proxy port for mobile access
     *
     * @param port Port number
     */
    public void setImsProxyPortForMobile(int port) {
        writeInteger(RcsSettingsData.IMS_PROXY_PORT_MOBILE, port);
    }

    /**
     * Get IMS proxy address for Wi-Fi access
     *
     * @return Address
     */
    public String getImsProxyAddrForWifi() {
        return readString(RcsSettingsData.IMS_PROXY_ADDR_WIFI);
    }

    /**
     * Set IMS proxy address for Wi-Fi access
     *
     * @param addr Address
     */
    public void setImsProxyAddrForWifi(String addr) {
        writeString(RcsSettingsData.IMS_PROXY_ADDR_WIFI, addr);
    }

    /**
     * Get IMS proxy port for Wi-Fi access
     *
     * @return Port
     */
    public int getImsProxyPortForWifi() {
        return readInteger(RcsSettingsData.IMS_PROXY_PORT_WIFI);
    }

    /**
     * Set IMS proxy port for Wi-Fi access
     *
     * @param port Port number
     */
    public void setImsProxyPortForWifi(int port) {
        writeInteger(RcsSettingsData.IMS_PROXY_PORT_WIFI, port);
    }

    /**
     * Get XDM server address
     *
     * @return Address as <host>:<port>/<root>
     */
    public Uri getXdmServer() {
        return readUri(RcsSettingsData.XDM_SERVER);
    }

    /**
     * Set XDM server address
     *
     * @param addr Address as <host>:<port>/<root>
     */
    public void setXdmServer(Uri addr) {
        writeUri(RcsSettingsData.XDM_SERVER, addr);
    }

    /**
     * Get XDM server login
     *
     * @return String value
     */
    public String getXdmLogin() {
        return readString(RcsSettingsData.XDM_LOGIN);
    }

    /**
     * Set XDM server login
     *
     * @param value Value
     */
    public void setXdmLogin(String value) {
        writeString(RcsSettingsData.XDM_LOGIN, value);
    }

    /**
     * Get XDM server password
     *
     * @return String value
     */
    public String getXdmPassword() {
        return readString(RcsSettingsData.XDM_PASSWORD);
    }

    /**
     * Set XDM server password
     *
     * @param value Value
     */
    public void setXdmPassword(String value) {
        writeString(RcsSettingsData.XDM_PASSWORD, value);
    }

    /**
     * Get file transfer HTTP server address
     *
     * @return Address
     */
    public Uri getFtHttpServer() {
        return readUri(RcsSettingsData.FT_HTTP_SERVER);
    }

    /**
     * Set file transfer HTTP server address
     *
     * @param addr Address
     */
    public void setFtHttpServer(Uri addr) {
        writeUri(RcsSettingsData.FT_HTTP_SERVER, addr);
    }

    /**
     * Get file transfer HTTP server login
     *
     * @return String value
     */
    public String getFtHttpLogin() {
        return readString(RcsSettingsData.FT_HTTP_LOGIN);
    }

    /**
     * Set file transfer HTTP server login
     *
     * @param value Value
     */
    public void setFtHttpLogin(String value) {
        writeString(RcsSettingsData.FT_HTTP_LOGIN, value);
    }

    /**
     * Get file transfer HTTP server password
     *
     * @return String value
     */
    public String getFtHttpPassword() {
        return readString(RcsSettingsData.FT_HTTP_PASSWORD);
    }

    /**
     * Set file transfer HTTP server password
     *
     * @param value Value
     */
    public void setFtHttpPassword(String value) {
        writeString(RcsSettingsData.FT_HTTP_PASSWORD, value);
    }

    /**
     * Get file transfer protocol
     *
     * @return FileTransferProtocol
     */
    public FileTransferProtocol getFtProtocol() {
        String protocol = readString(RcsSettingsData.FT_PROTOCOL);
        return FileTransferProtocol.valueOf(protocol);
    }

    /**
     * Set file transfer protocol
     *
     * @param protocol the protocol
     */
    public void setFtProtocol(FileTransferProtocol protocol) {
        writeString(RcsSettingsData.FT_PROTOCOL, protocol.name());
    }

    /**
     * Get IM mass URI
     *
     * @return SIP-URI
     */
    public Uri getImMassUri() {
        return readUri(RcsSettingsData.IM_MASS_URI);
    }

    /**
     * Set IM mass URI
     *
     * @param uri SIP-URI
     */
    public void setImMassUri(Uri uri) {
        writeUri(RcsSettingsData.IM_MASS_URI, uri);
    }

    /**
     * Get IM conference URI
     *
     * @return SIP-URI
     */
    public Uri getImConferenceUri() {
        return readUri(RcsSettingsData.IM_CONF_URI);
    }

    /**
     * Set IM conference URI
     *
     * @param uri SIP-URI
     */
    public void setImConferenceUri(Uri uri) {
        writeUri(RcsSettingsData.IM_CONF_URI, uri);
    }

    /**
     * Get end user confirmation request URI
     *
     * @return SIP-URI
     */
    public Uri getEndUserConfirmationRequestUri() {
        return readUri(RcsSettingsData.ENDUSER_CONFIRMATION_URI);
    }

    /**
     * Set end user confirmation request
     *
     * @param uri SIP-URI
     */
    public void setEndUserConfirmationRequestUri(Uri uri) {
        writeUri(RcsSettingsData.ENDUSER_CONFIRMATION_URI, uri);
    }

    /**
     * Get message store server address
     *
     * @return Address as <host>:<port>
     */
    public Uri getMsgStoreServer() {
        return readUri(RcsSettingsData.MSG_STORE_SERVER);
    }

    /**
     * Set message store server address
     *
     * @param addr Address as <host>:<port>
     */
    public void setMsgStoreServer(Uri addr) {
        writeUri(RcsSettingsData.MSG_STORE_SERVER, addr);
    }

    /**
     * Get profile server address
     *
     * @return Address as <host>:<port>
     */
    public Uri getProfileServer() {
        return readUri(RcsSettingsData.PROFILE_SERVER);
    }

    /**
     * Set profile server address
     *
     * @param addr Address as <host>:<port>
     */
    public void setProfileServer(Uri addr) {
        writeUri(RcsSettingsData.PROFILE_SERVER, addr);
    }

    /**
     * Get public account server address
     *
     * @return Address as <host>:<port>
     */
    public Uri getPublicAccountServer() {
        return readUri(RcsSettingsData.PUBLICACCOUNT_SERVER);
    }

    /**
     * Set public account server address
     *
     * @param addr Address as <host>:<port>
     */
    public void setPublicAccountServer(Uri addr) {
        writeUri(RcsSettingsData.PUBLICACCOUNT_SERVER, addr);
    }

    /**
     * Get SSO server address
     *
     * @return Address as <host>:<port>
     */
    public Uri getSsoServer() {
        return readUri(RcsSettingsData.SSO_SERVER);
    }

    /**
     * Set SSO server address
     *
     * @param addr Address as <host>:<port>
     */
    public void setSsoServer(Uri addr) {
        writeUri(RcsSettingsData.SSO_SERVER, addr);
    }


    /**
     * Get qrcard server address
     *
     * @return Address as <host>:<port>
     */
    public Uri getQrcardServer() {
        return readUri(RcsSettingsData.QRCARD_SERVER);
    }

    /**
     * Set qrcard server address
     *
     * @param addr Address as <host>:<port>
     */
    public void setQrcardServer(Uri addr) {
        writeUri(RcsSettingsData.QRCARD_SERVER, addr);
    }


    /**
     * Get pc application server address
     *
     * @return Address as <host>:<port>
     */
    public Uri getPcApplicationServer() {
        return readUri(RcsSettingsData.PC_APPLICATION_SERVER);
    }

    /**
     * Set pc application server address
     *
     * @param addr Address as <host>:<port>
     */
    public void setPcApplicationServer(Uri addr) {
        writeUri(RcsSettingsData.PC_APPLICATION_SERVER, addr);
    }

    /**
     * Get my capabilities
     *
     * @return capability
     */
    public Capabilities getMyCapabilities() {
        /* Initialize with default capabilities */
        CapabilitiesBuilder capaBuilder = new CapabilitiesBuilder();
        /* Add my own capabilities */
        capaBuilder.setCsVideo(isCsVideoSupported());
        capaBuilder.setFileTransferMsrp(isFileTransferSupported());
        capaBuilder.setFileTransferHttp(isFileTransferHttpSupported());
        capaBuilder.setImageSharing(isImageSharingSupported());
        capaBuilder.setStandaloneMessaging(isStandaloneMessagingSupported());
        capaBuilder.setImSession(isImSessionSupported());
        capaBuilder.setPresenceDiscovery(isPresenceDiscoverySupported());
        capaBuilder.setSocialPresence(isSocialPresenceSupported());
        capaBuilder.setVideoSharing(isVideoSharingSupported());
        capaBuilder.setGeolocationPush(isGeoLocationPushSupported());
        capaBuilder.setFileTransferThumbnail(isFileTransferThumbnailSupported());
        capaBuilder.setFileTransferStoreForward(isFileTransferStoreForwardSupported());
        capaBuilder.setIpVoiceCall(isIPVoiceCallSupported());
        capaBuilder.setIpVideoCall(isIPVideoCallSupported());
        capaBuilder.setGroupChatStoreForward(isGroupChatStoreForwardSupported());
        capaBuilder.setSipAutomata(isSipAutomata());
        capaBuilder.setTimestampOfLastRequest(Capabilities.INVALID_TIMESTAMP);
        capaBuilder.setTimestampOfLastResponse(Capabilities.INVALID_TIMESTAMP);
        /* Add extensions */
        capaBuilder.setExtensions(getSupportedRcsExtensions());
        return capaBuilder.build();
    }

    /**
     * Get max photo-icon size
     *
     * @return Size in bytes
     */
    public long getMaxPhotoIconSize() {
        return readLong(RcsSettingsData.MAX_PHOTO_ICON_SIZE);
    }

    /**
     * Sets max photo-icon size
     *
     * @param size the maximum photo icon size
     */
    public void setMaxPhotoIconSize(long size) {
        writeLong(RcsSettingsData.MAX_PHOTO_ICON_SIZE, size);
    }

    /**
     * Get max number of participants in a group chat
     *
     * @return Number of participants
     */
    public int getMaxChatParticipants() {
        return readInteger(RcsSettingsData.MAX_CHAT_PARTICIPANTS);
    }

    /**
     * @return minimum number of participants in a Group Chat
     */
    public int getMinGroupChatParticipants() {
        return MINIMUM_GC_PARTICIPANTS;
    }

    /**
     * Gets max length of a standalone message
     *
     * @return Number of char
     */
    public int getMaxStandaloneMsgLength() {
        return readInteger(RcsSettingsData.MAX_STANDALONE_MSG_LENGTH);
    }

    /**
     * Get max length of a chat message
     *
     * @return Number of char
     */
    public int getMaxChatMessageLength() {
        return readInteger(RcsSettingsData.MAX_CHAT_MSG_LENGTH);
    }

    /**
     * Get max length of a group chat message
     *
     * @return Number of char
     */
    public int getMaxGroupChatMessageLength() {
        return readInteger(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH);
    }

    /**
     * Get idle duration of a chat session
     *
     * @return Duration in milliseconds
     */
    public long getChatIdleDuration() {
        return readLong(RcsSettingsData.CHAT_IDLE_DURATION);
    }

    /**
     * Get max file transfer size
     *
     * @return Size in bytes
     */
    public long getMaxFileTransferSize() {
        return readLong(RcsSettingsData.MAX_FILE_TRANSFER_SIZE);
    }

    /**
     * Sets warning threshold for max file transfer size
     *
     * @param size the maximum file transfer size
     */
    public void setMaxFileTransferSize(long size) {
        writeLong(RcsSettingsData.MAX_FILE_TRANSFER_SIZE, size);
    }

    /**
     * Get warning threshold for max file transfer size
     *
     * @return Size in bytes
     */
    public long getWarningMaxFileTransferSize() {
        return readLong(RcsSettingsData.WARN_FILE_TRANSFER_SIZE);
    }

    /**
     * Sets warning threshold for max file transfer size
     *
     * @param size the warning threshold for file transfer size
     */
    public void setWarningMaxFileTransferSize(long size) {
        writeLong(RcsSettingsData.WARN_FILE_TRANSFER_SIZE, size);
    }

    /**
     * Gets the max image share size
     *
     * @return Size in bytes
     */
    public long getMaxImageSharingSize() {
        return readLong(RcsSettingsData.MAX_IMAGE_SHARE_SIZE);
    }

    /**
     * Sets the max image share size
     *
     * @param size the maximum image sharing size
     */
    public void setMaxImageSharingSize(long size) {
        writeLong(RcsSettingsData.MAX_IMAGE_SHARE_SIZE, size);
    }

    /**
     * Get max duration of a video share
     *
     * @return Duration in milliseconds
     */
    public long getMaxVideoShareDuration() {
        return readLong(RcsSettingsData.MAX_VIDEO_SHARE_DURATION);
    }

    /**
     * Get max audio message duration of a video share
     *
     * @return Duration in milliseconds
     */
    public long getMaxAudioMessageDuration() {
        return readLong(RcsSettingsData.MAX_AUDIO_MESSAGE_DURATION);
    }

    /**
     * Get max number of simultaneous chat sessions
     *
     * @return Number of sessions
     */
    public int getMaxChatSessions() {
        return readInteger(RcsSettingsData.MAX_CHAT_SESSIONS);
    }

    /**
     * Get max number of simultaneous file transfer sessions
     *
     * @return Number of sessions
     */
    public int getMaxFileTransferSessions() {
        return readInteger(RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS);
    }

    /**
     * Get max number of simultaneous outgoing file transfer sessions
     *
     * @return Number of sessions
     */
    public int getMaxConcurrentOutgoingFileTransferSessions() {
        return readInteger(RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS);
    }

    /**
     * Is SMS fallback service activated
     *
     * @return Boolean
     */
    public boolean isSmsFallbackServiceActivated() {
        return readBoolean(RcsSettingsData.SMS_FALLBACK_SERVICE);
    }

    /**
     * Is chat invitation auto accepted
     *
     * @return Boolean
     */
    public boolean isChatAutoAccepted() {
        return readBoolean(RcsSettingsData.AUTO_ACCEPT_CHAT);
    }

    /**
     * Is group chat invitation auto accepted
     *
     * @return Boolean
     */
    public boolean isGroupChatAutoAccepted() {
        return readBoolean(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT);
    }

    /**
     * Is file transfer invitation auto accepted
     *
     * @return Boolean
     */
    public boolean isFileTransferAutoAccepted() {
        return readBoolean(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER);
    }

    /**
     * Is Store & Forward service warning activated
     *
     * @return Boolean
     */
    public boolean isStoreForwardWarningActivated() {
        return readBoolean(RcsSettingsData.WARN_SF_SERVICE);
    }

    /**
     * Get IM session start mode
     *
     * @return the IM session start mode
     *         <p/>
     *         <ul>
     *         <li>0 (RCS-e default): The 200 OK is sent when the receiver consumes the notification
     *         opening the chat window.
     *         <li>1 (RCS default): The 200 OK is sent when the receiver starts to type a message
     *         back in the chat window.
     *         <li>2: The 200 OK is sent when the receiver presses the button to send a message
     *         (that is the message will be buffered in the client until the MSRP session is
     *         established). Note: as described in section 3.2, the parameter only affects the
     *         behavior for 1-to-1 sessions in case no session between the parties has been
     *         established yet.
     *         </ul>
     */
    public ImSessionStartMode getImSessionStartMode() {
        return ImSessionStartMode.valueOf(readInteger(RcsSettingsData.IM_SESSION_START));
    }

    /**
     * Set IM session start mode
     *
     * @param mode IM session start mode
     */
    public void setImSessionStartMode(ImSessionStartMode mode) {
        writeInteger(RcsSettingsData.IM_SESSION_START, mode.toInt());
    }

    /**
     * Get polling period used before each IMS service check (e.g. test subscription state for
     * presence service)
     *
     * @return Period in milliseconds
     */
    public long getImsServicePollingPeriod() {
        return readLong(RcsSettingsData.IMS_SERVICE_POLLING_PERIOD);
    }

    /**
     * Get default SIP listening port
     *
     * @return Port
     */
    public int getSipListeningPort() {
        return readInteger(RcsSettingsData.SIP_DEFAULT_PORT);
    }

    /**
     * Get default SIP protocol for mobile
     *
     * @return Protocol (udp | tcp | tls)
     */
    public String getSipDefaultProtocolForMobile() {
        return readString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE);
    }

    /**
     * Get default SIP protocol for wifi
     *
     * @return Protocol (udp | tcp | tls)
     */
    public String getSipDefaultProtocolForWifi() {
        return readString(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI);
    }

    /**
     * Get TLS Certificate root
     *
     * @return Path of the certificate
     */
    public String getTlsCertificateRoot() {
        return readString(RcsSettingsData.TLS_CERTIFICATE_ROOT);
    }

    /**
     * Get TLS Certificate intermediate
     *
     * @return Path of the certificate
     */
    public String getTlsCertificateIntermediate() {
        return readString(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE);
    }

    /**
     * Get SIP transaction timeout used to wait SIP response
     *
     * @return Timeout in milliseconds
     */
    public long getSipTransactionTimeout() {
        return readLong(RcsSettingsData.SIP_TRANSACTION_TIMEOUT);
    }

    /**
     * Get default MSRP port
     *
     * @return Port
     */
    public int getDefaultMsrpPort() {
        return readInteger(RcsSettingsData.MSRP_DEFAULT_PORT);
    }

    /**
     * Get default RTP port
     *
     * @return Port
     */
    public int getDefaultRtpPort() {
        return readInteger(RcsSettingsData.RTP_DEFAULT_PORT);
    }

    /**
     * Get MSRP transaction timeout used to wait MSRP response
     *
     * @return Timeout in milliseconds
     */
    public long getMsrpTransactionTimeout() {
        return readLong(RcsSettingsData.MSRP_TRANSACTION_TIMEOUT);
    }

    /**
     * Get default expire period for REGISTER
     *
     * @return Period in milliseconds
     */
    public long getRegisterExpirePeriod() {
        return readLong(RcsSettingsData.REGISTER_EXPIRE_PERIOD);
    }

    /**
     * Get registration retry base time
     *
     * @return Time in milliseconds
     */
    public long getRegisterRetryBaseTime() {
        return readLong(RcsSettingsData.REGISTER_RETRY_BASE_TIME);
    }

    /**
     * Get registration retry max time
     *
     * @return Time in milliseconds
     */
    public long getRegisterRetryMaxTime() {
        return readLong(RcsSettingsData.REGISTER_RETRY_MAX_TIME);
    }

    /**
     * Get default expire period for PUBLISH
     *
     * @return Period in milliseconds
     */
    public long getPublishExpirePeriod() {
        return readLong(RcsSettingsData.PUBLISH_EXPIRE_PERIOD);
    }

    /**
     * Get IMS authentication procedure for mobile access
     *
     * @return Authentication procedure
     */
    public AuthenticationProcedure getImsAuthenticationProcedureForMobile() {
        return AuthenticationProcedure
                .valueOf(readString(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE));
    }

    /**
     * Set the authentication procedure for mobile
     *
     * @param procedure the procedure
     */
    public void setImsAuthenticationProcedureForMobile(AuthenticationProcedure procedure) {
        writeString(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE, procedure.name());
    }

    /**
     * Get IMS authentication procedure for Wi-Fi access
     *
     * @return Authentication procedure
     */
    public AuthenticationProcedure getImsAuthenticationProcedureForWifi() {
        return AuthenticationProcedure
                .valueOf(readString(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI));
    }

    /**
     * Set the authentication procedure for Wi-Fi
     *
     * @param procedure the procedure
     */
    public void setImsAuhtenticationProcedureForWifi(AuthenticationProcedure procedure) {
        writeString(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI, procedure.name());
    }

    /**
     * Is Tel-URI format used
     *
     * @return Boolean
     */
    public boolean isTelUriFormatUsed() {
        return readBoolean(RcsSettingsData.TEL_URI_FORMAT);
    }

    /**
     * Get ringing period
     *
     * @return Period in milliseconds
     */
    public long getRingingPeriod() {
        return readLong(RcsSettingsData.RINGING_SESSION_PERIOD);
    }

    /**
     * Get default expire period for SUBSCRIBE
     *
     * @return Period in milliseconds
     */
    public long getSubscribeExpirePeriod() {
        return readLong(RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD);
    }

    /**
     * Get "Is-composing" timeout for chat service
     *
     * @return Timer in milliseconds
     */
    public long getIsComposingTimeout() {
        return readLong(RcsSettingsData.IS_COMPOSING_TIMEOUT);
    }

    /**
     * Get default expire period for INVITE (session refresh)
     *
     * @return Period in milliseconds
     */
    public long getSessionRefreshExpirePeriod() {
        return readLong(RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD);
    }

    /**
     * Is permanente state mode activated
     *
     * @return Boolean
     */
    public boolean isPermanentStateModeActivated() {
        return readBoolean(RcsSettingsData.PERMANENT_STATE_MODE);
    }

    /**
     * Is trace activated
     *
     * @return Boolean
     */
    public boolean isTraceActivated() {
        return readBoolean(RcsSettingsData.TRACE_ACTIVATED);
    }

    /**
     * Get trace level
     *
     * @return trace level
     */
    public int getTraceLevel() {
        return readInteger(RcsSettingsData.TRACE_LEVEL);
    }

    /**
     * Is media trace activated
     *
     * @return Boolean
     */
    public boolean isSipTraceActivated() {
        return readBoolean(RcsSettingsData.SIP_TRACE_ACTIVATED);
    }

    /**
     * Get SIP trace file
     *
     * @return SIP trace file
     */
    public String getSipTraceFile() {
        return readString(RcsSettingsData.SIP_TRACE_FILE);
    }

    /**
     * Is media trace activated
     *
     * @return Boolean
     */
    public boolean isMediaTraceActivated() {
        return readBoolean(RcsSettingsData.MEDIA_TRACE_ACTIVATED);
    }

    /**
     * Get capability refresh timeout used to avoid too many requests in a short time
     *
     * @return Timeout in milliseconds
     */
    public long getCapabilityRefreshTimeout() {
        return readLong(RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT);
    }

    /**
     * Get capability expiry timeout used to decide when to refresh contact capabilities
     *
     * @return Timeout in milliseconds
     */
    public long getCapabilityExpiryTimeout() {
        return readLong(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT);
    }

    /**
     * Get capability polling period used to refresh contacts capabilities
     *
     * @return Timeout in milliseconds
     */
    public long getCapabilityPollingPeriod() {
        return readLong(RcsSettingsData.CAPABILITY_POLLING_PERIOD);
    }

    /**
     * Is CS video supported
     *
     * @return Boolean
     */
    public boolean isCsVideoSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_CS_VIDEO);
    }

    /**
     * Is file transfer supported
     *
     * @return Boolean
     */
    public boolean isFileTransferSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER);
    }

    /**
     * Set HTTP file transfer support
     *
     * @param supported true if HTTP file transfer is supported
     */
    public void setFileTransferHttpSupported(boolean supported) {
        writeBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP, supported);
    }

    /**
     * Is file transfer via HTTP supported
     *
     * @return Boolean
     */
    public boolean isFileTransferHttpSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP);
    }

    /**
     * Is standalone messaging supported
     *
     * @return Boolean
     */
    public boolean isStandaloneMessagingSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_STANDALONE_MESSAGING);
    }

    /**
     * Is IM session supported
     *
     * @return Boolean
     */
    public boolean isImSessionSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_IM_SESSION);
    }

    /**
     * Is IM group session supported
     *
     * @return Boolean
     */
    public boolean isImGroupSessionSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_IM_GROUP_SESSION);
    }

    /**
     * Is image sharing supported
     *
     * @return Boolean
     */
    public boolean isImageSharingSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_IMAGE_SHARING);
    }

    /**
     * Is video sharing supported
     *
     * @return Boolean
     */
    public boolean isVideoSharingSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_VIDEO_SHARING);
    }

    /**
     * Is presence discovery supported
     *
     * @return Boolean
     */
    public boolean isPresenceDiscoverySupported() {
        return getXdmServer() != null && readBoolean(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY);
    }

    /**
     * Is social presence supported
     *
     * @return Boolean
     */
    public boolean isSocialPresenceSupported() {
        return getXdmServer() != null && readBoolean(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE);
    }

    /**
     * Is geolocation push supported
     *
     * @return Boolean
     */
    public boolean isGeoLocationPushSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH);
    }

    /**
     * Is file transfer thumbnail supported
     *
     * @return Boolean
     */
    public boolean isFileTransferThumbnailSupported() {
        // Thumbnail is only supported in HTPP.
        // The thumbnail configuration settings is fixed 0 for MSRP per specification.
        // Refer to PDD v3 page 106.
        // return isFileTransferHttpSupported()
        return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL);
    }

    /**
     * Is file transfer Store & Forward supported
     *
     * @return Boolean
     */
    public boolean isFileTransferStoreForwardSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF);
    }

    /**
     * Is IP voice call supported
     *
     * @return Boolean
     */
    public boolean isIPVoiceCallSupported() {
        // TODO: Add Ipcall support here in future releases
        // return readBoolean(RcsSettingsData.CAPABILITY_IP_VOICE_CALL);
        return false;
    }

    /**
     * Is IP video call supported
     *
     * @return Boolean
     */
    public boolean isIPVideoCallSupported() {
        // TODO: Add Ipcall support here in future releases
        // return readBoolean(RcsSettingsData.CAPABILITY_IP_VIDEO_CALL);
        return false;
    }

    /**
     * Is group chat Store & Forward supported
     *
     * @return Boolean
     */
    public boolean isGroupChatStoreForwardSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_GROUP_CHAT_SF);
    }

    /**
     * Is invite only if group chat Store & Forward supported
     *
     * @return Boolean
     */
    public boolean isGroupChatInviteIfFullStoreForwardSupported() {
        return readBoolean(RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF);
    }

    /**
     * Get set of supported RCS extensions
     *
     * @return the set of extensions
     */
    public Set<String> getSupportedRcsExtensions() {
        return ServiceExtensionManager
                .getExtensions(readString(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS));
    }

    /**
     * Set the set of supported RCS extensions
     *
     * @param extensions Set of extensions
     */
    public void setSupportedRcsExtensions(Set<String> extensions) {
        writeString(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS,
                ServiceExtensionManager.getExtensions(extensions));
    }

    /**
     * Is call composer supported
     *
     * @return Boolean
     */
    public boolean isCallComposerSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_CALL_COMPOSER);
    }

    /**
     * Is shared map supported
     *
     * @return Boolean
     */
    public boolean isSharedMapSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_SHARED_MAP);
    }

    /**
     * Is shared sketch supported
     *
     * @return Boolean
     */
    public boolean isSharedSketchSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_SHARED_SKETCH);
    }

    /**
     * Is post call supported
     *
     * @return Boolean
     */
    public boolean isPostCallSupported() {
        return readBoolean(RcsSettingsData.CAPABILITY_POST_CALL);
    }

    /**
     * Is IM always-on thanks to the Store & Forward functionality
     *
     * @return Boolean
     */
    public boolean isImAlwaysOn() {
        return readBoolean(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON);
    }

    /**
     * Is File Transfer always-on thanks to the Store & Forward functionality
     *
     * @return Boolean
     */
    public boolean isFtAlwaysOn() {
        return readBoolean(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON);
    }

    /**
     * Get FT Http cap always on option
     *
     * @return result ( Default = false = OFF and true = ON )
     */
    public boolean isFtHttpCapAlwaysOn() {
        return readBoolean(RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON);
    }

    /**
     * Is IM reports activated
     *
     * @return Boolean
     */
    public boolean isImReportsActivated() {
        return readBoolean(RcsSettingsData.IM_USE_REPORTS);
    }

    /**
     * Get network access
     *
     * @return Network type
     */
    public NetworkAccessType getNetworkAccess() {
        return NetworkAccessType.valueOf(readInteger(RcsSettingsData.NETWORK_ACCESS));
    }

    /**
     * Set network access type
     *
     * @param networkAccess the network access
     */
    public void setNetworkAccess(NetworkAccessType networkAccess) {
        writeInteger(RcsSettingsData.NETWORK_ACCESS, networkAccess.toInt());
    }

    /**
     * Get SIP timer T1
     *
     * @return Timer in milliseconds
     */
    public long getSipTimerT1() {
        return readLong(RcsSettingsData.SIP_TIMER_T1);
    }

    /**
     * Get SIP timer T2
     *
     * @return Timer in milliseconds
     */
    public long getSipTimerT2() {
        return readLong(RcsSettingsData.SIP_TIMER_T2);
    }

    /**
     * Get SIP timer T4
     *
     * @return Timer in milliseconds
     */
    public long getSipTimerT4() {
        return readLong(RcsSettingsData.SIP_TIMER_T4);
    }

    /**
     * Is SIP keep-alive enabled
     *
     * @return Boolean
     */
    public boolean isSipKeepAliveEnabled() {
        return readBoolean(RcsSettingsData.SIP_KEEP_ALIVE);
    }

    /**
     * Get SIP keep-alive period
     *
     * @return Period in milliseconds
     */
    public long getSipKeepAlivePeriod() {
        return readLong(RcsSettingsData.SIP_KEEP_ALIVE_PERIOD);
    }

    /**
     * Get operator authorized to connect to RCS platform
     *
     * @return SIM operator name (null means any SIM operator is authorized to connect to RCS)
     */
    public String getNetworkOperator() {
        return readString(RcsSettingsData.RCS_OPERATOR);
    }

    /**
     * Is GRUU supported
     *
     * @return Boolean
     */
    public boolean isGruuSupported() {
        return readBoolean(RcsSettingsData.GRUU);
    }

    /**
     * Is CPU Always_on activated
     *
     * @return Boolean
     */
    public boolean isCpuAlwaysOn() {
        return readBoolean(RcsSettingsData.CPU_ALWAYS_ON);
    }

    /**
     * Get configuration mode
     *
     * @return Mode MANUAL | AUTO
     */
    public ConfigurationMode getConfigurationMode() {
        return ConfigurationMode.valueOf(readInteger(RcsSettingsData.CONFIG_MODE));
    }

    /**
     * Set configuration mode
     *
     * @param mode MANUAL | AUTO
     */
    public void setConfigurationMode(ConfigurationMode mode) {
        writeInteger(RcsSettingsData.CONFIG_MODE, mode.toInt());
    }

    /**
     * Get terms and conditions via provisioning response
     *
     * @return TermsAndConditionsResponse
     */
    public TermsAndConditionsResponse getTermsAndConditionsResponse() {
        return TermsAndConditionsResponse.valueOf(readInteger(RcsSettingsData.TC_RESPONSE));
    }

    /**
     * Get provisioning version
     *
     * @return Version
     */
    public int getProvisioningVersion() {
        return readInteger(RcsSettingsData.PROVISIONING_VERSION);
    }

    /**
     * Set provisioning version
     *
     * @param version Version
     */
    public void setProvisioningVersion(int version) {
        writeInteger(RcsSettingsData.PROVISIONING_VERSION, version);
    }

    /**
     * Set terms and conditions response
     *
     * @param resp The terms and conditions response
     */
    public void setTermsAndConditionsResponse(TermsAndConditionsResponse resp) {
        writeInteger(RcsSettingsData.TC_RESPONSE, resp.toInt());
    }

    /**
     * Get secondary provisioning address
     *
     * @return Address
     */
    public String getSecondaryProvisioningAddress() {
        return readString(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS);
    }

    /**
     * Is secondary provisioning address only used
     *
     * @return Boolean
     */
    public boolean isSecondaryProvisioningAddressOnly() {
        return readBoolean(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY);
    }

    /**
     * Reset configuration parameters to default values
     */
    public void resetConfigParameters() {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();
        for (Map.Entry<String, Object> entry : RcsSettingsData.sSettingsKeyDefaultValue.entrySet()) {
            operations.add(buildContentProviderOp(entry.getKey(), entry.getValue()));
        }
        try {
            mCache.clear();
            mLocalContentResolver.applyBatch(RcsSettingsData.CONTENT_URI, operations);

        } catch (OperationApplicationException e) {
            sLogger.error("Reset existing configuration failed", e);
        }
    }

    /**
     * Is user profile configured
     *
     * @return Returns true if the configuration is valid
     */
    public boolean isUserProfileConfigured() {
        // Check platform settings
        if (TextUtils.isEmpty(getImsProxyAddrForMobile())) {
            return false;
        }

        // Check user profile settings
        if (TextUtils.isEmpty(getUserProfileImsDomain())) {
            return false;
        }
        AuthenticationProcedure mode = getImsAuthenticationProcedureForMobile();
        switch (mode) {
            case AKA:
            case DIGEST:
                if (getUserProfileImsUserName() == null) {
                    return false;
                }
                if (TextUtils.isEmpty(getUserProfileImsPassword())) {
                    return false;
                }
                if (TextUtils.isEmpty(getUserProfileImsPrivateId())) {
                    return false;
                }
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * Is group chat activated
     *
     * @return Boolean
     */
    public boolean isGroupChatActivated() {
        return !RcsSettingsData.DEFAULT_GROUP_CHAT_URI.equals(getImConferenceUri());
    }

    /**
     * Get the root directory for photos
     *
     * @return Directory path
     */
    public String getPhotoRootDirectory() {
        return readString(RcsSettingsData.DIRECTORY_PATH_PHOTOS);
    }

    /**
     * Get the root directory for videos
     *
     * @return Directory path
     */
    public String getVideoRootDirectory() {
        return readString(RcsSettingsData.DIRECTORY_PATH_VIDEOS);
    }

    public String getAudioRootDirectory() {
        return readString(RcsSettingsData.DIRECTORY_PATH_AUDIOS);
    }

    /**
     * Get the root directory for files
     *
     * @return Directory path
     */
    public String getFileRootDirectory() {
        return readString(RcsSettingsData.DIRECTORY_PATH_FILES);
    }

    /**
     * Is secure MSRP media over Mobile access
     *
     * @return Boolean
     */
    public boolean isSecureMsrpOverMobile() {
        return readBoolean(RcsSettingsData.SECURE_MSRP_OVER_MOBILE);
    }

    /**
     * Is secure MSRP media over Wi-Fi
     *
     * @return Boolean
     */
    public boolean isSecureMsrpOverWifi() {
        return readBoolean(RcsSettingsData.SECURE_MSRP_OVER_WIFI);
    }

    /**
     * Get the root directory for file icons
     *
     * @return Directory path
     */
    public String getFileIconRootDirectory() {
        return readString(RcsSettingsData.DIRECTORY_PATH_FILEICONS);
    }

    public ImMsgTech getImMsgTech() {
        return ImMsgTech.valueOf(readInteger(RcsSettingsData.IM_MSG_TECH));
    }

    public void setImMsgTech(ImMsgTech mode) {
        writeInteger(RcsSettingsData.IM_MSG_TECH, mode.toInt());
    }

    public boolean isCpmMsgTech() {
        return ImMsgTech.CPM.equals(getImMsgTech());
    }

    public boolean isFirstMessageInInvite() {
        return readBoolean(RcsSettingsData.FIRST_MESSAGE_INVITE);
    }

    public void setFirstMessageInInvite(boolean inInvite) {
        writeBoolean(RcsSettingsData.FIRST_MESSAGE_INVITE, inInvite);
    }

    /**
     * Is secure RTP media over Mobile access
     *
     * @return Boolean
     */
    public boolean isSecureRtpOverMobile() {
        return readBoolean(RcsSettingsData.SECURE_RTP_OVER_MOBILE);
    }

    /**
     * Is secure RTP media over Wi-Fi
     *
     * @return Boolean
     */
    public boolean isSecureRtpOverWifi() {
        return readBoolean(RcsSettingsData.SECURE_RTP_OVER_WIFI);
    }

    /**
     * Get max geolocation label length
     *
     * @return Number of char
     */
    public int getMaxGeolocLabelLength() {
        return readInteger(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH);
    }

    /**
     * Get geolocation expiration time
     *
     * @return Time in milliseconds
     */
    public long getGeolocExpirationTime() {
        return readLong(RcsSettingsData.GEOLOC_EXPIRATION_TIME);
    }

    /**
     * Set provisioning token
     *
     * @param token the token
     */
    public void setProvisioningToken(String token) {
        writeString(RcsSettingsData.PROVISIONING_TOKEN, token);
    }

    /**
     * @return provisioning token
     */
    public String getProvisioningToken() {
        return readString(RcsSettingsData.PROVISIONING_TOKEN);
    }

    /**
     * Is SIP device an automata ?
     *
     * @return Boolean
     */
    public boolean isSipAutomata() {
        return readBoolean(RcsSettingsData.CAPABILITY_SIP_AUTOMATA);
    }

    /**
     * Get max file-icon size
     *
     * @return Size in bytes
     */
    public long getMaxFileIconSize() {
        return readLong(RcsSettingsData.MAX_FILE_ICON_SIZE);
    }

    /**
     * Get the GSMA release
     *
     * @return the GSMA release
     */
    public GsmaRelease getGsmaRelease() {
        return GsmaRelease.valueOf(readInteger(RcsSettingsData.KEY_GSMA_RELEASE));
    }

    /**
     * Set the GSMA release
     *
     * @param release Release
     */
    public void setGsmaRelease(GsmaRelease release) {
        writeInteger(RcsSettingsData.KEY_GSMA_RELEASE, release.toInt());
    }

    /**
     * Is Albatros GSMA release
     *
     * @return Boolean
     */
    public boolean isAlbatrosRelease() {
        return (GsmaRelease.ALBATROS.equals(getGsmaRelease()));
    }

    /**
     * Is China mobile release
     *
     * @return Boolean
     */
    public boolean isCmccRelease() {
        return (GsmaRelease.CHINA_MOBILE.equals(getGsmaRelease()));
    }

    /**
     * Is TCP fallback enabled according to RFC3261 chapter 18.1.1
     *
     * @return Boolean
     */
    public boolean isTcpFallback() {
        return readBoolean(RcsSettingsData.TCP_FALLBACK);
    }

    /**
     * Is RCS extensions allowed
     *
     * @return Boolean
     */
    public boolean isExtensionsAllowed() {
        return readBoolean(RcsSettingsData.ALLOW_EXTENSIONS);
    }

    /**
     * Is RCS extension authorized
     *
     * @return Boolean
     */
    public boolean isExtensionAuthorized(String ext) {
        return ext != null
                && !(EnrichCallingService.CALL_COMPOSER_FEATURE_TAG.equals(ext) && !isCallComposerSupported())
                && !(EnrichCallingService.SHARED_MAP_SERVICE_ID.equals(ext) && !isSharedMapSupported())
                && !(EnrichCallingService.SHARED_SKETCH_SERVICE_ID.equals(ext) && !isSharedSketchSupported())
                && !(EnrichCallingService.POST_CALL_SERVICE_ID.equals(ext) && !isPostCallSupported());
    }

    /**
     * Get max lenght for extensions using real time messaging (MSRP)
     *
     * @return Max length
     */
    public int getMaxMsrpLengthForExtensions() {
        return readInteger(RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS);
    }

    /**
     * Get call composer inactivity timeout
     *
     * @return Timeout in milliseconds
     */
    public long getCallComposerInactivityTimeout() {
        return readLong(RcsSettingsData.CALL_COMPOSER_INACTIVITY_TIMEOUT);
    }

    /**
     * Set the client messaging mode
     *
     * @param mode the client messaging mode (0: CONVERGED, 1: INTEGRATED, 2: SEAMLESS, 3: NONE)
     */
    public void setMessagingMode(MessagingMode mode) {
        writeInteger(RcsSettingsData.MESSAGING_MODE, mode.toInt());
    }

    /**
     * Get the client messaging mode
     *
     * @return the client messaging mode (0: CONVERGED, 1: INTEGRATED, 2: SEAMLESS, 3: NONE)
     */
    public MessagingMode getMessagingMode() {
        return MessagingMode.valueOf(readInteger(RcsSettingsData.MESSAGING_MODE));
    }

    /**
     * Is file transfer invitation auto accepted in roaming
     *
     * @return Boolean
     */
    public boolean isFileTransferAutoAcceptedInRoaming() {
        return readBoolean(RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING);
    }

    /**
     * Set File Transfer Auto Accepted in roaming
     *
     * @param option Option
     */
    public void setFileTransferAutoAcceptedInRoaming(boolean option) {
        writeBoolean(RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING, option);
    }

    /**
     * Set File Transfer Auto Accepted in normal conditions
     *
     * @param option Option
     */
    public void setFileTransferAutoAccepted(boolean option) {
        writeBoolean(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER, option);
    }

    /**
     * Is file transfer invitation auto accepted enabled (by the network)
     *
     * @return Boolean
     */
    public boolean isFtAutoAcceptedModeChangeable() {
        return readBoolean(RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE);
    }

    /**
     * Set File Transfer Auto Accepted Mode changeable option
     *
     * @param option Option
     */
    public void setFtAutoAcceptedModeChangeable(boolean option) {
        writeBoolean(RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE, option);
    }

    /**
     * returns the image resize option for file transfer in the range [ALWAYS_PERFORM,
     * ONLY_ABOVE_MAX_SIZE, ASK]
     *
     * @return image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
     */
    public ImageResizeOption getImageResizeOption() {
        return ImageResizeOption.valueOf(readInteger(RcsSettingsData.KEY_IMAGE_RESIZE_OPTION));
    }

    /**
     * Set the image resize option
     *
     * @param option the image resize option (0: ALWAYS_PERFORM, 1: ONLY_ABOVE_MAX_SIZE, 2: ASK)
     */
    public void setImageResizeOption(ImageResizeOption option) {
        writeInteger(RcsSettingsData.KEY_IMAGE_RESIZE_OPTION, option.toInt());
    }

    /**
     * Get the default messaging method
     *
     * @return the default messaging method (0: AUTOMATIC, 1: RCS, 2: NON_RCS)
     */
    public MessagingMethod getDefaultMessagingMethod() {
        return MessagingMethod.valueOf(readInteger(RcsSettingsData.DEFAULT_MESSAGING_METHOD));
    }

    /**
     * Set default messaging method
     *
     * @param method the default messaging method (0: AUTOMATIC, 1: RCS, 2: NON_RCS)
     */
    public void setDefaultMessagingMethod(MessagingMethod method) {
        writeInteger(RcsSettingsData.DEFAULT_MESSAGING_METHOD, method.toInt());
    }

    /**
     * Is configuration valid
     *
     * @return Boolean
     */
    public boolean isConfigurationValid() {
        return readBoolean(RcsSettingsData.CONFIGURATION_VALID);
    }

    /**
     * Set configuration valid
     *
     * @param valid true if configuration is valid
     */
    public void setConfigurationValid(boolean valid) {
        writeBoolean(RcsSettingsData.CONFIGURATION_VALID, valid);
    }

    /**
     * @return the maximum length of subject in Group Chat
     */
    public int getGroupChatSubjectMaxLength() {
        return GROUP_CHAT_SUBJECT_MAX_LENGTH;
    }

    /**
     * Sets RCS activation changeable by the client applications
     *
     * @param enableSwitch the enable switch
     */
    public void setEnableRcseSwitch(EnableRcseSwitch enableSwitch) {
        writeInteger(RcsSettingsData.ENABLE_RCS_SWITCH, enableSwitch.toInt());
    }

    /**
     * Returns how to show the RCS enabled/disabled switch
     *
     * @return EnableRcseSwitch
     */
    public EnableRcseSwitch getEnableRcseSwitch() {
        return EnableRcseSwitch.valueOf(readInteger(RcsSettingsData.ENABLE_RCS_SWITCH));
    }

    /**
     * Get contact cap validity period in one-one messaging
     *
     * @return Period in milliseconds
     */
    public long getMsgCapValidityPeriod() {
        return readLong(RcsSettingsData.MSG_CAP_VALIDITY_PERIOD);
    }

    /**
     * Get group imdn display reports option
     *
     * @return True if group display reports are to be requested and responded to
     */
    public boolean isRequestAndRespondToGroupDisplayReportsEnabled() {
        return readBoolean(RcsSettingsData.REQUEST_AND_RESPOND_TO_GROUP_DISPLAY_REPORTS);
    }

    /**
     * Get message delivery timeout in one-one messaging
     *
     * @return Period in milliseconds
     */
    public long getMsgDeliveryTimeoutPeriod() {
        return readLong(RcsSettingsData.MSG_DELIVERY_TIMEOUT);
    }

    /**
     * Restrict display name length to 256 characters, as allowing infinite length string as display
     * name will eventually crash the stack.
     *
     * @return the maximum characters allowed for display name
     */
    public int getMaxAllowedDisplayNameChars() {
        return readInteger(RcsSettingsData.MAX_ALLOWED_DISPLAY_NAME_CHARS);
    }

    /**
     * Sets the user message content
     *
     * @param message the user message content
     */
    public void setProvisioningUserMessageContent(String message) {
        writeString(RcsSettingsData.PROV_USER_MSG_CONTENT, message);
    }

    /**
     * Gets the user message content
     *
     * @return the user message content
     */
    public String getProvisioningUserMessageContent() {
        return readString(RcsSettingsData.PROV_USER_MSG_CONTENT);
    }

    /**
     * Sets the user message title
     *
     * @param title the user message title
     */
    public void setProvisioningUserMessageTitle(String title) {
        writeString(RcsSettingsData.PROV_USER_MSG_TITLE, title);
    }

    /**
     * Gets the user message title
     *
     * @return the user message title
     */
    public String getProvisioningUserMessageTitle() {
        return readString(RcsSettingsData.PROV_USER_MSG_TITLE);
    }

    /**
     * Get mobile country code
     *
     * @return mobile country code or 0 if undefined
     */
    public int getMobileCountryCode() {
        return readInteger(RcsSettingsData.MOBILE_COUNTRY_CODE);
    }

    /**
     * Set the mobile country code
     *
     * @param mcc the mobile country code
     */
    public void setMobileCountryCode(int mcc) {
        writeInteger(RcsSettingsData.MOBILE_COUNTRY_CODE, mcc);
    }

    /**
     * Get mobile network code
     *
     * @return mobile network code or 0 if undefined
     */
    public int getMobileNetworkCode() {
        return readInteger(RcsSettingsData.MOBILE_NETWORK_CODE);
    }

    /**
     * Set the mobile network code
     *
     * @param mnc the mobile network code
     */
    public void setMobileNetworkCode(int mnc) {
        writeInteger(RcsSettingsData.MOBILE_NETWORK_CODE, mnc);
    }

    /**
     * Sets whether an Accept button is shown with the message in the terms and conditions pop-up
     *
     * @param accept True if an Accept button is shown with the message in the terms and conditions
     *            pop-up
     */
    public void setProvisioningAcceptButton(boolean accept) {
        writeBoolean(RcsSettingsData.PROV_ACCEPT_BUTTON, accept);
    }

    /**
     * Is Accept button shown with the message in the terms and conditions pop-up
     *
     * @return Boolean True if Accept button shown with the message in the terms and conditions
     *         pop-up
     */
    public boolean isProvisioningAcceptButton() {
        return readBoolean(RcsSettingsData.PROV_ACCEPT_BUTTON);
    }

    /**
     * Sets whether a Decline button is shown with the message in the terms and conditions pop-up
     *
     * @param reject True if a Decline button is shown with the message in the terms and conditions
     *            pop-up
     */
    public void setProvisioningRejectButton(boolean reject) {
        writeBoolean(RcsSettingsData.PROV_REJECT_BUTTON, reject);
    }

    /**
     * Is Decline button shown with the message in the terms and conditions pop-up
     *
     * @return Boolean True if a Decline button is shown with the message in the terms and
     *         conditions pop-up
     */
    public boolean isProvisioningRejectButton() {
        return readBoolean(RcsSettingsData.PROV_REJECT_BUTTON);
    }

    /**
     * Sets the display language
     *
     * @param language the display language
     */
    public void setDisplayLanguage(String language) {
        writeString(RcsSettingsData.LOCAL_DISPLAY_LANGUAGE, language);
    }

    /**
     * Gets the display language
     *
     * @return the display language
     */
    public String getDisplayLanguage() {
        return readString(RcsSettingsData.LOCAL_DISPLAY_LANGUAGE);
    }

    /**
     * Is enrich calling service supported
     *
     * @return Boolean True if supported
     */
    public boolean isEnrichCallingServiceSupported() {
        return readBoolean(RcsSettingsData.ENRICH_CALLING_SERVICE);
    }

}
