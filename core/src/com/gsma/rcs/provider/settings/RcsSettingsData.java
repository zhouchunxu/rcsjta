/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010-2016 Orange.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMethod;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransferServiceConfiguration.ImageResizeOption;

import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Environment;
import android.util.SparseArray;

import java.util.HashMap;
import java.util.Map;

import javax2.sip.ListeningPoint;

/**
 * RCS settings data constants
 *
 * @author jexa7410
 * @author yplo6403
 */
public class RcsSettingsData {
    /**
     * Content provider URI
     */
    /* package private */static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.setting/setting");

    /**
     * Key of the Rcs configuration parameter
     */
    /* package private */static final String KEY_KEY = "key";

    /**
     * Value of the Rcs configuration parameter
     */
    /* package private */static final String KEY_VALUE = "value";

    /**
     * Default group chat conference URI
     */
    public static final Uri DEFAULT_GROUP_CHAT_URI = Uri.parse("sip:foo@bar");

    /**
     * File type for certificate
     */
    public static final String CERTIFICATE_FILE_TYPE = ".crt";

    // ---------------------------------------------------------------------------
    // Enumerated
    // ---------------------------------------------------------------------------

    /**
     * The authentication procedure enumerated type.
     */
    public enum AuthenticationProcedure {
        /**
         * GIBA authentication
         */
        GIBA(0),
        /**
         * Digest authentication
         */
        DIGEST(1),
        /**
         * IMS AKA authentication
         */
        AKA(2);

        private final int mValue;

        private static SparseArray<AuthenticationProcedure> mValueToEnum = new SparseArray<>();

        static {
            for (AuthenticationProcedure entry : AuthenticationProcedure.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        AuthenticationProcedure(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the authentication procedure
         * @return AuthenticationProcedure
         */
        public static AuthenticationProcedure valueOf(int value) {
            AuthenticationProcedure entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + AuthenticationProcedure.class.getName() + "." + value);
        }

    }

    /**
     * Option for what ux-operation to react on when handling manual acceptance of one2one and group
     * chat invitations.
     */
    public enum ImSessionStartMode {
        /**
         * Group chat session is accepted when opening conversation
         */
        ON_OPENING(0),
        /**
         * Group chat session is accepted when composing first message
         */
        ON_COMPOSING(1),
        /**
         * Group chat session is accepted when sending first message
         */
        ON_SENDING(2);

        private final int mValue;

        private static SparseArray<ImSessionStartMode> mValueToEnum = new SparseArray<>();

        static {
            for (ImSessionStartMode entry : ImSessionStartMode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        ImSessionStartMode(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the IM session start mode
         * @return ImSessionStartMode
         */
        public static ImSessionStartMode valueOf(int value) {
            ImSessionStartMode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + ImSessionStartMode.class.getName() + "." + value);
        }

    }

    public enum TermsAndConditionsResponse {

        NO_ANSWER(0), DECLINED(1), ACCEPTED(2);

        private final int mValue;

        private static SparseArray<TermsAndConditionsResponse> mValueToEnum = new SparseArray<>();

        static {
            for (TermsAndConditionsResponse entry : TermsAndConditionsResponse.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        TermsAndConditionsResponse(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the Terms and Condition response
         * @return TermAndConditionsResponse
         */
        public static TermsAndConditionsResponse valueOf(int value) {
            TermsAndConditionsResponse entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + TermsAndConditionsResponse.class.getName() + "." + value);
        }

    }

    public enum ImMsgTech {

        SIMPLE_IM(0),

        CPM(1);

        private final int mValue;

        private static SparseArray<ImMsgTech> mValueToEnum = new SparseArray<>();

        static {
            for (ImMsgTech entry : ImMsgTech.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        ImMsgTech(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the IM message tech
         * @return ImMsgTech
         */
        public static ImMsgTech valueOf(int value) {
            ImMsgTech entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + ImMsgTech.class.getName()
                    + "." + value);
        }

    }

    /**
     * Network access type
     */
    public enum NetworkAccessType {
        /**
         * Mobile access type
         */
        MOBILE(ConnectivityManager.TYPE_MOBILE),
        /**
         * Wifi access type
         */
        WIFI(ConnectivityManager.TYPE_WIFI),
        /**
         * All access types
         */
        ANY(-1);

        private int mValue;

        private static SparseArray<NetworkAccessType> mValueToEnum = new SparseArray<>();

        static {
            for (NetworkAccessType entry : NetworkAccessType.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        NetworkAccessType(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the network access type.
         * @return NetworkAccessType
         */
        public static NetworkAccessType valueOf(int value) {
            NetworkAccessType entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + NetworkAccessType.class.getName() + "." + value);
        }

    }

    /**
     * EnableRcseSwitch describes whether or not to show the RCS enabled/disabled switch permanently
     */
    public enum EnableRcseSwitch {
        /**
         * the switch is shown permanently
         */
        ALWAYS_SHOW(1),
        /**
         * the switch is only shown during roaming
         */
        ONLY_SHOW_IN_ROAMING(0),
        /**
         * the switch is never shown
         */
        NEVER_SHOW(-1);

        private int mValue;

        private static SparseArray<EnableRcseSwitch> mValueToEnum = new SparseArray<>();

        static {
            for (EnableRcseSwitch entry : EnableRcseSwitch.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        EnableRcseSwitch(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the enable RCSe switch
         * @return EnableRcseSwitch
         */
        public static EnableRcseSwitch valueOf(int value) {
            EnableRcseSwitch entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + EnableRcseSwitch.class.getName() + "." + value);
        }

    }

    /**
     * The configuration mode enumerated type.
     */
    public enum ConfigurationMode {
        /**
         * Manual configuration
         */
        MANUAL(0),
        /**
         * Automatic configuration
         */
        AUTO(1);

        private int mValue;

        private static SparseArray<ConfigurationMode> mValueToEnum = new SparseArray<>();

        static {
            for (ConfigurationMode entry : ConfigurationMode.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        ConfigurationMode(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the configuration mode
         * @return ConfigurationMode
         */
        public static ConfigurationMode valueOf(int value) {
            ConfigurationMode entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + ConfigurationMode.class.getName() + "." + value);
        }
    }

    /**
     * The File Transfer protocol enumerated type.
     */
    public enum FileTransferProtocol {
        /**
         * MSRP protocol
         */
        MSRP,
        /**
         * HTTP protocol
         */
        HTTP
    }

    /**
     * The GSMA release enumerated type.
     */
    public enum GsmaRelease {
        /**
         * Albatros release
         */
        ALBATROS(0),
        /**
         * Blackbird release
         */
        BLACKBIRD(1),
        /**
         * Crane release
         */
        CRANE(2),
        /**
         * China Mobile custom release
         */
        CHINA_MOBILE(3);

        private int mValue;

        private static SparseArray<GsmaRelease> mValueToEnum = new SparseArray<>();

        static {
            for (GsmaRelease entry : GsmaRelease.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        GsmaRelease(int value) {
            mValue = value;
        }

        /**
         * @return value
         */
        public final int toInt() {
            return mValue;
        }

        /**
         * @param value the value representing the GSMA release
         * @return GsmaRelease
         */
        public static GsmaRelease valueOf(int value) {
            GsmaRelease entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class " + GsmaRelease.class.getName()
                    + "." + value);
        }

    }

    // ---------------------------------------------------------------------------
    // UI settings
    // ---------------------------------------------------------------------------

    /**
     * Activate or not the RCS service
     */
    public static final String SERVICE_ACTIVATED = "ServiceActivated";
    /* package private */static final Boolean DEFAULT_SERVICE_ACTIVATED = true;

    /**
     * Send or not the displayed notification
     */
    public static final String CHAT_RESPOND_TO_DISPLAY_REPORTS = "ChatRespondToDisplayReports";
    /* package private */static final Boolean DEFAULT_CHAT_RESPOND_TO_DISPLAY_REPORTS = true;

    /**
     * Battery level minimum
     */
    /* package private */static final String MIN_BATTERY_LEVEL = "MinBatteryLevel";
    /* package private */static final Integer DEFAULT_MIN_BATTERY_LEVEL = 0;

    // ---------------------------------------------------------------------------
    // Service settings
    // ---------------------------------------------------------------------------

    /**
     * Max file-icon size
     */
    public static final String MAX_FILE_ICON_SIZE = "FileIconSize";
    /* package private */static final Long DEFAULT_MAX_FILE_ICON_SIZE = 50L * 1024L;

    /**
     * Max photo-icon size
     */
    public static final String MAX_PHOTO_ICON_SIZE = "MaxPhotoIconSize";
    /* package private */static final Long DEFAULT_MAX_PHOTO_ICON_SIZE = 100L * 1024L;

    /**
     * Max length of the freetext
     */
    public static final String MAX_FREETXT_LENGTH = "MaxFreetextLength";
    /* package private */static final Integer DEFAULT_MAX_FREETXT_LENGTH = 100;

    /**
     * Max number of participants in a group chat
     */
    public static final String MAX_CHAT_PARTICIPANTS = "MaxChatParticipants";
    /* package private */static final Integer DEFAULT_MAX_CHAT_PARTICIPANTS = 10;

    /**
     * Max length of a standalone message
     */
    public static final String MAX_STANDALONE_MSG_LENGTH = "MaxStandaloneMsgLength";
    /* package private */static final Integer DEFAULT_MAX_STANDALONE_MSG_LENGTH = 100;

    /**
     * Max length of a chat message
     */
    public static final String MAX_CHAT_MSG_LENGTH = "MaxChatMessageLength";
    /* package private */static final Integer DEFAULT_MAX_CHAT_MSG_LENGTH = 100;

    /**
     * Max length of a group chat message
     */
    public static final String MAX_GROUPCHAT_MSG_LENGTH = "MaxGroupChatMessageLength";
    /* package private */static final Integer DEFAULT_MAX_GC_MSG_LENGTH = 100;

    /**
     * Idle duration of a chat session in milliseconds
     */
    public static final String CHAT_IDLE_DURATION = "ChatIdleDuration";
    /* package private */static final Long DEFAULT_CHAT_IDLE_DURATION = 300000L;

    /**
     * Max size of a file transfer
     */
    public static final String MAX_FILE_TRANSFER_SIZE = "MaxFileTransferSize";
    /* package private */static final Long DEFAULT_MAX_FT_SIZE = 3072L * 1024L;

    /**
     * Warning threshold for file transfer size
     */
    public static final String WARN_FILE_TRANSFER_SIZE = "WarnFileTransferSize";
    /* package private */static final Long DEFAULT_WARN_FT_SIZE = 2048L * 1024L;

    /**
     * Max size of an image share
     */
    public static final String MAX_IMAGE_SHARE_SIZE = "MaxImageShareSize";
    /* package private */static final Long DEFAULT_MAX_ISH_SIZE = 3072L * 1024L;

    /**
     * Max duration of a video share in milliseconds
     */
    public static final String MAX_VIDEO_SHARE_DURATION = "MaxVideoShareDuration";
    /* package private */static final Long DEFAULT_MAX_VSH_DURATION = 54000000L;

    /**
     * Max Audio Message Duration in millisecond
     */
    public static final String MAX_AUDIO_MESSAGE_DURATION = "MaxAudioMessageDuration";
    /* package private */static final Long DEFAULT_MAX_AUDIO_DURATION = 600000L;

    /**
     * Max number of simultaneous chat sessions
     */
    public static final String MAX_CHAT_SESSIONS = "MaxChatSessions";
    /* package private */static final Integer DEFAULT_MAX_CHAT_SESSIONS = 20;

    /**
     * Max number of simultaneous file transfer sessions
     */
    public static final String MAX_FILE_TRANSFER_SESSIONS = "MaxFileTransferSessions";
    /* package private */static final Integer DEFAULT_MAX_FT_SESSIONS = 10;

    /**
     * Max number of simultaneous outgoing file transfer sessions allowed
     */
    public static final String MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS = "MaxConcurrentOutgoingFileTransferSessions";
    /* package private */static final Integer DEFAULT_MAX_CONCURRENT_OUTGOING_FT_SESSIONS = 1;

    /**
     * Max number of simultaneous IP call sessions
     */
    public static final String MAX_IP_CALL_SESSIONS = "MaxIpCallSessions";
    /* package private */static final Integer DEFAULT_MAX_IP_CALL_SESSIONS = 5;

    /**
     * Activate or not SMS fallback service
     */
    public static final String SMS_FALLBACK_SERVICE = "SmsFallbackService";
    /* package private */static final Boolean DEFAULT_SMS_FALLBACK_SERVICE = true;

    /**
     * Auto accept file transfer invitation
     */
    public static final String AUTO_ACCEPT_FILE_TRANSFER = "AutoAcceptFileTransfer";
    /* package private */static final Boolean DEFAULT_AUTO_ACCEPT_FT = false;

    /**
     * Auto accept chat invitation
     */
    public static final String AUTO_ACCEPT_CHAT = "AutoAcceptChat";
    /* package private */static final Boolean DEFAULT_AUTO_ACCEPT_CHAT = false;

    /**
     * Auto accept group chat invitation
     */
    public static final String AUTO_ACCEPT_GROUP_CHAT = "AutoAcceptGroupChat";
    /* package private */static final Boolean DEFAULT_AUTO_ACCEPT_GC = false;

    /**
     * Display a warning if Store & Forward service is activated
     */
    public static final String WARN_SF_SERVICE = "StoreForwardServiceWarning";
    /* package private */static final Boolean DEFAULT_WARN_SF_SERVICE = false;

    /**
     * Define when the chat receiver sends the 200 OK back to the sender
     */
    public static final String IM_SESSION_START = "ImSessionStart";
    /* package private */static final Integer DEFAULT_IM_SESSION_START = 1;

    /**
     * Max entries for chat log
     */
    public static final String MAX_CHAT_LOG_ENTRIES = "MaxChatLogEntries";
    /* package private */static final Integer DEFAULT_MAX_CHAT_LOG_ENTRIES = 500;

    /**
     * Max entries for richcall log
     */
    public static final String MAX_RICHCALL_LOG_ENTRIES = "MaxRichcallLogEntries";
    /* package private */static final Integer DEFAULT_MAX_RICHCALL_LOG_ENTRIES = 200;

    /**
     * Max entries for IP call log
     */
    public static final String MAX_IPCALL_LOG_ENTRIES = "MaxIpcallLogEntries";
    /* package private */static final Integer DEFAULT_MAX_IPCALL_LOG_ENTRIES = 200;

    /**
     * Max length of a geolocation label
     */
    public static final String MAX_GEOLOC_LABEL_LENGTH = "MaxGeolocLabelLength";
    /* package private */static final Integer DEFAULT_MAX_GEOLOC_LABEL_LENGTH = 100;

    /**
     * Geolocation expiration time
     */
    public static final String GEOLOC_EXPIRATION_TIME = "GeolocExpirationTime";
    /* package private */static final Long DEFAULT_GEOLOC_EXPIRATION_TIME = 3600000L;

    /**
     * Convergent messaging UX option
     */
    public static final String MESSAGING_MODE = "MessagingMode";
    /* package private */static final Integer DEFAULT_KEY_MESSAGING_MODE = MessagingMode.NONE
            .toInt();

    /**
     * Default messaging method
     */
    public static final String DEFAULT_MESSAGING_METHOD = "DefaultMessagingMethod";
    /* package private */static final Integer DEFAULT_KEY_DEFAULT_MESSAGING_METHOD = MessagingMethod.AUTOMATIC
            .toInt();

    /**
     * Enrich calling service support
     */
    public static final String ENRICH_CALLING_SERVICE = "EnrichCallingSupport";
    /* package private */static final Boolean DEFAULT_ENRICH_CALLING_SERVICE = true;

    // ---------------------------------------------------------------------------
    // User profile settings
    // ---------------------------------------------------------------------------

    /**
     * IMS username or username part of the IMPU (for HTTP Digest only)
     */
    public static final String USERPROFILE_IMS_USERNAME = "MyContactId";
    /* package private */static final ContactId DEFAULT_USERPROFILE_IMS_USERNAME = null;

    /**
     * IMS display name
     */
    public static final String USERPROFILE_IMS_DISPLAY_NAME = "MyDisplayName";
    /* package private */static final String DEFAULT_USERPROFILE_IMS_DISPLAY_NAME = null;

    /**
     * IMS home domain
     */
    public static final String USERPROFILE_IMS_HOME_DOMAIN = "ImsHomeDomain";
    /* package private */static final String DEFAULT_USERPROFILE_IMS_HOME_DOMAIN = null;

    /**
     * IMS public URI of PC
     */
    public static final String USERPROFILE_IMS_PUBLIC_ID_PC = "ImsPublicIdPc";
    /* package private */static final ContactId DEFAULT_USERPROFILE_IMS_PUBLIC_ID_PC = null;

    /**
     * IMS private URI or IMPI (for HTTP Digest only)
     */
    public static final String USERPROFILE_IMS_PRIVATE_ID = "ImsPrivateId";
    /* package private */static final String DEFAULT_USERPROFILE_IMS_PRIVATE_ID = null;

    /**
     * IMS password (for HTTP Digest only)
     */
    public static final String USERPROFILE_IMS_PASSWORD = "ImsPassword";
    /* package private */static final String DEFAULT_USERPROFILE_IMS_PASSWORD = null;

    /**
     * IMS realm (for HTTP Digest only)
     */
    public static final String USERPROFILE_IMS_REALM = "ImsRealm";
    /* package private */static final String DEFAULT_USERPROFILE_IMS_REALM = null;

    /**
     * P-CSCF or outbound proxy address for mobile access
     */
    public static final String IMS_PROXY_ADDR_MOBILE = "ImsOutboundProxyAddrForMobile";
    /* package private */static final String DEFAULT_IMS_PROXY_ADDR_MOBILE = null;

    /**
     * P-CSCF or outbound proxy port for mobile access
     */
    public static final String IMS_PROXY_PORT_MOBILE = "ImsOutboundProxyPortForMobile";
    /* package private */static final Integer DEFAULT_IMS_PROXY_PORT_MOBILE = 5060;

    /**
     * P-CSCF or outbound proxy address for Wi-Fi access
     */
    public static final String IMS_PROXY_ADDR_WIFI = "ImsOutboundProxyAddrForWifi";
    /* package private */static final String DEFAULT_IMS_PROXY_ADDR_WIFI = null;

    /**
     * P-CSCF or outbound proxy port for Wi-Fi access
     */
    public static final String IMS_PROXY_PORT_WIFI = "ImsOutboundProxyPortForWifi";
    /* package private */static final Integer DEFAULT_IMS_PROXY_PORT_WIFI = 5060;

    /**
     * XDM server address & port
     */
    public static final String XDM_SERVER = "XdmServerAddr";
    /* package private */static final Uri DEFAULT_XDM_SERVER = null;

    /**
     * XDM server login (for HTTP Digest only)
     */
    public static final String XDM_LOGIN = "XdmServerLogin";
    /* package private */static final String DEFAULT_XDM_LOGIN = null;

    /**
     * XDM server password (for HTTP Digest only)
     */
    public static final String XDM_PASSWORD = "XdmServerPassword";
    /* package private */static final String DEFAULT_XDM_PASSWORD = null;

    /**
     * File transfer HTTP server address & port
     */
    public static final String FT_HTTP_SERVER = "FtHttpServerAddr";
    /* package private */static final Uri DEFAULT_FT_HTTP_SERVER = null;

    /**
     * File transfer HTTP server login
     */
    public static final String FT_HTTP_LOGIN = "FtHttpServerLogin";
    /* package private */static final String DEFAULT_FT_HTTP_LOGIN = null;

    /**
     * File transfer HTTP server password
     */
    public static final String FT_HTTP_PASSWORD = "FtHttpServerPassword";
    /* package private */static final String DEFAULT_FT_HTTP_PASSWORD = null;

    /**
     * File transfer default protocol
     */
    public static final String FT_PROTOCOL = "FtProtocol";
    /* package private */static final String DEFAULT_FT_PROTOCOL = FileTransferProtocol.MSRP.name();

    /**
     * IM mass URI for standalone messaging (1-N)
     */
    public static final String IM_MASS_URI = "ImMassUri";
    /* package private */static final Uri DEFAULT_IM_MASS_URI = null;

    /**
     * IM conference URI for group chat session
     */
    public static final String IM_CONF_URI = "ImConferenceUri";
    /* package private */static final Uri DEFAULT_IM_CONF_URI = RcsSettingsData.DEFAULT_GROUP_CHAT_URI;

    /**
     * End user confirmation request URI for terms and conditions
     */
    public static final String ENDUSER_CONFIRMATION_URI = "EndUserConfReqUri";
    /* package private */static final String DEFAULT_ENDUSER_CONFIRMATION_URI = null;

    /**
     * Message store server adddress & port
     */
    public static final String MSG_STORE_SERVER = "MsgStoreServerAddr";
    /* package private */static final Uri DEFAULT_MSG_STORE_SERVER = null;

    /**
     * Profile server address & port
     */
    public static final String PROFILE_SERVER = "ProfileServerAddr";
    /* package private */static final Uri DEFAULT_PROFILE_SERVER = null;

    /**
     * Public account server address & port
     */
    public static final String PUBLICACCOUNT_SERVER = "PublicAccountServerAddr";
    /* package private */static final Uri DEFAULT_PUBLICACCOUNT_SERVER = null;

    /**
     * SSO server address & port
     */
    public static final String SSO_SERVER = "SsoServerAddr";
    /* package private */static final Uri DEFAULT_SSO_SERVER = null;

    /**
     * QRCARD server address & port
     */
    public static final String QRCARD_SERVER = "QrcardServerAddr";
    /* package private */static final Uri DEFAULT_QRCARD_SERVER = null;

    /**
     * Pc application server address & port
     */
    public static final String PC_APPLICATION_SERVER = "PcApplicationServerAddr";
    /* package private */static final Uri DEFAULT_PC_APPLICATION_SERVER = null;

    /**
     * UUID value for populating SIP instance
     */
    public static final String UUID = "UUID";
    /* package private */static final String DEFAULT_UUID = null;

    // ---------------------------------------------------------------------------
    // Stack settings
    // ---------------------------------------------------------------------------

    /**
     * Polling period used before each IMS service check (e.g. test subscription state for presence
     * service)
     */
    public static final String IMS_SERVICE_POLLING_PERIOD = "ImsServicePollingPeriod";
    /* package private */static final Long DEFAULT_IMS_SERVICE_POLLING_PERIOD = 30000L;

    /**
     * Default SIP port
     */
    public static final String SIP_DEFAULT_PORT = "SipListeningPort";
    /* package private */static final Integer DEFAULT_SIP_DEFAULT_PORT = 5062;

    /**
     * Default SIP protocol for mobile
     */
    public static final String SIP_DEFAULT_PROTOCOL_FOR_MOBILE = "SipDefaultProtocolForMobile";
    /* package private */static final String DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_MOBILE = ListeningPoint.UDP;

    /**
     * Default SIP protocol for Wi-Fi
     */
    public static final String SIP_DEFAULT_PROTOCOL_FOR_WIFI = "SipDefaultProtocolForWifi";
    /* package private */static final String DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_WIFI = ListeningPoint.TCP;

    /**
     * TLS Certificate root
     */
    public static final String TLS_CERTIFICATE_ROOT = "TlsCertificateRoot";
    /* package private */static final String DEFAULT_TLS_CERTIFICATE_ROOT = null;

    /**
     * TLS Certificate intermediate
     */
    public static final String TLS_CERTIFICATE_INTERMEDIATE = "TlsCertificateIntermediate";
    /* package private */static final String DEFAULT_TLS_CERTIFICATE_INTERMEDIATE = null;

    /**
     * SIP transaction timeout used to wait a SIP response in milliseconds
     */
    public static final String SIP_TRANSACTION_TIMEOUT = "SipTransactionTimeout";
    /* package private */static final Long DEFAULT_SIP_TRANSACTION_TIMEOUT = 120000L;

    /**
     * Default TCP port for MSRP session
     */
    public static final String MSRP_DEFAULT_PORT = "DefaultMsrpPort";
    /* package private */static final Integer DEFAULT_MSRP_DEFAULT_PORT = 20000;

    /**
     * Default UDP port for RTP session
     */
    public static final String RTP_DEFAULT_PORT = "DefaultRtpPort";
    /* package private */static final Integer DEFAULT_RTP_DEFAULT_PORT = 10000;

    /**
     * MSRP transaction timeout used to wait MSRP response in milliseconds
     */
    public static final String MSRP_TRANSACTION_TIMEOUT = "MsrpTransactionTimeout";
    /* package private */static final Long DEFAULT_MSRP_TRANSACTION_TIMEOUT = 5000L;

    /**
     * Registration expire period
     */
    public static final String REGISTER_EXPIRE_PERIOD = "RegisterExpirePeriod";
    /* package private */static final Long DEFAULT_REGISTER_EXPIRE_PERIOD = 600000000L;

    /**
     * Registration retry base time
     */
    public static final String REGISTER_RETRY_BASE_TIME = "RegisterRetryBaseTime";
    /* package private */static final Long DEFAULT_REGISTER_RETRY_BASE_TIME = 30000L;

    /**
     * Registration retry max time
     */
    public static final String REGISTER_RETRY_MAX_TIME = "RegisterRetryMaxTime";
    /* package private */static final Long DEFAULT_REGISTER_RETRY_MAX_TIME = 1800000L;

    /**
     * Publish expire period
     */
    public static final String PUBLISH_EXPIRE_PERIOD = "PublishExpirePeriod";
    /* package private */static final Long DEFAULT_PUBLISH_EXPIRE_PERIOD = 3600000L;

    /**
     * Revoke timeout in milliseconds
     */
    public static final String REVOKE_TIMEOUT = "RevokeTimeout";
    /* package private */static final Long DEFAULT_REVOKE_TIMEOUT = 300000L;

    /**
     * IMS authentication procedure for mobile access
     */
    public static final String IMS_AUTHENT_PROCEDURE_MOBILE = "ImsAuthenticationProcedureForMobile";
    /* package private */static final String DEFAULT_IMS_AUTHENT_PROCEDURE_MOBILE = AuthenticationProcedure.DIGEST
            .name();

    /**
     * IMS authentication procedure for Wi-Fi access
     */
    public static final String IMS_AUTHENT_PROCEDURE_WIFI = "ImsAuthenticationProcedureForWifi";
    /* package private */static final String DEFAULT_IMS_AUTHENT_PROCEDURE_WIFI = AuthenticationProcedure.DIGEST
            .name();

    /**
     * Activate or not Tel-URI format
     */
    public static final String TEL_URI_FORMAT = "TelUriFormat";
    /* package private */static final Boolean DEFAULT_TEL_URI_FORMAT = true;

    /**
     * Ringing session period in milliseconds. At the end of the period the session is cancelled
     */
    public static final String RINGING_SESSION_PERIOD = "RingingPeriod";
    /* package private */static final Long DEFAULT_RINGING_SESSION_PERIOD = 60000L;

    /**
     * Subscribe expiration period
     */
    public static final String SUBSCRIBE_EXPIRE_PERIOD = "SubscribeExpirePeriod";
    /* package private */static final Long DEFAULT_SUBSCRIBE_EXPIRE_PERIOD = 3600000L;

    /**
     * "Is-composing" timeout milliseconds for chat service
     */
    public static final String IS_COMPOSING_TIMEOUT = "IsComposingTimeout";
    /* package private */static final Long DEFAULT_IS_COMPOSING_TIMEOUT = 15000L;

    /**
     * SIP session refresh expire period
     */
    public static final String SESSION_REFRESH_EXPIRE_PERIOD = "SessionRefreshExpirePeriod";
    /* package private */static final Long DEFAULT_SESSION_REFRESH_EXPIRE_PERIOD = 0L;

    /**
     * Activate or not permanent state mode
     */
    public static final String PERMANENT_STATE_MODE = "PermanentState";
    /* package private */static final Boolean DEFAULT_PERMANENT_STATE_MODE = true;

    /**
     * Activate or not the traces
     */
    public static final String TRACE_ACTIVATED = "TraceActivated";
    /* package private */static final Boolean DEFAULT_TRACE_ACTIVATED = true;

    /**
     * Logger trace level
     */
    public static final String TRACE_LEVEL = "TraceLevel";
    /* package private */static final Integer DEFAULT_TRACE_LEVEL = Logger.DEBUG_LEVEL;

    /**
     * Activate or not the SIP trace
     */
    public static final String SIP_TRACE_ACTIVATED = "SipTraceActivated";
    /* package private */static final Boolean DEFAULT_SIP_TRACE_ACTIVATED = false;

    /**
     * SIP trace file
     */
    public static final String SIP_TRACE_FILE = "SipTraceFile";
    /* package private */static final String DEFAULT_SIP_TRACE_FILE = Environment
            .getExternalStorageDirectory() + "/sip.txt";

    /**
     * Activate or not the media trace
     */
    public static final String MEDIA_TRACE_ACTIVATED = "MediaTraceActivated";
    /* package private */static final Boolean DEFAULT_MEDIA_TRACE_ACTIVATED = false;

    /**
     * Capability refresh timeout used to avoid too many requests in a short time
     */
    public static final String CAPABILITY_REFRESH_TIMEOUT = "CapabilityRefreshTimeout";
    /* package private */static final Long DEFAULT_CAPABILITY_REFRESH_TIMEOUT = 1000L;

    /**
     * Capability refresh timeout used to decide when to refresh contact capabilities
     */
    public static final String CAPABILITY_EXPIRY_TIMEOUT = "CapabilityExpiryTimeout";
    /* package private */static final Long DEFAULT_CAPABILITY_EXPIRY_TIMEOUT = 86400000L;

    /**
     * Polling period used to decide when to refresh contacts capabilities
     */
    public static final String CAPABILITY_POLLING_PERIOD = "CapabilityPollingPeriod";
    /* package private */static final Long DEFAULT_CAPABILITY_POLLING_PERIOD = 3600000L;

    /**
     * CS video capability
     */
    public static final String CAPABILITY_CS_VIDEO = "CapabilityCsVideo";
    /* package private */static final Boolean DEFAULT_CAPABILITY_CS_VIDEO = false;

    /**
     * Image sharing capability
     */
    public static final String CAPABILITY_IMAGE_SHARING = "CapabilityImageShare";
    /* package private */static final Boolean DEFAULT_CAPABILITY_ISH = false;

    /**
     * Video sharing capability
     */
    public static final String CAPABILITY_VIDEO_SHARING = "CapabilityVideoShare";
    /* package private */static final Boolean DEFAULT_CAPABILITY_VSH = false;

    /**
     * IP voice call capability
     */
    public static final String CAPABILITY_IP_VOICE_CALL = "CapabilityIPVoiceCall";
    /* package private */static final Boolean DEFAULT_CAPABILITY_IP_VOICE_CALL = false;

    /**
     * IP video call capability
     */
    public static final String CAPABILITY_IP_VIDEO_CALL = "CapabilityIPVideoCall";
    /* package private */static final Boolean DEFAULT_CAPABILITY_IP_VIDEO_CALL = false;

    /**
     * Standalone messaging capability
     */
    public static final String CAPABILITY_STANDALONE_MESSAGING = "CapabilityStandaloneMessaging";
    /* package private */static final Boolean DEFAULT_CAPABILITY_STANDALONE_MESSAGING = false;

    /**
     * Instant Messaging session capability
     */
    public static final String CAPABILITY_IM_SESSION = "CapabilityImSession";
    /* package private */static final Boolean DEFAULT_CAPABILITY_IM_SESSION = false;

    /**
     * Group Instant Messaging session capability
     */
    public static final String CAPABILITY_IM_GROUP_SESSION = "CapabilityImGroupSession";
    /* package private */static final Boolean DEFAULT_CAPABILITY_IM_GROUP_SESSION = true;

    /**
     * File transfer capability
     */
    public static final String CAPABILITY_FILE_TRANSFER = "CapabilityFileTransfer";
    /* package private */static final Boolean DEFAULT_CAPABILITY_FT = false;

    /**
     * File transfer via HTTP capability
     */
    public static final String CAPABILITY_FILE_TRANSFER_HTTP = "CapabilityFileTransferHttp";
    /* package private */static final Boolean DEFAULT_CAPABILITY_FT_HTTP = false;

    /**
     * Presence discovery capability
     */
    public static final String CAPABILITY_PRESENCE_DISCOVERY = "CapabilityPresenceDiscovery";
    /* package private */static final Boolean DEFAULT_CAPABILITY_PRESENCE_DISCOVERY = false;

    /**
     * Social presence capability
     */
    public static final String CAPABILITY_SOCIAL_PRESENCE = "CapabilitySocialPresence";
    /* package private */static final Boolean DEFAULT_CAPABILITY_SOCIAL_PRESENCE = false;

    /**
     * Geolocation push capability
     */
    public static final String CAPABILITY_GEOLOCATION_PUSH = "CapabilityGeoLocationPush";
    /* package private */static final Boolean DEFAULT_CAPABILITY_GEOLOCATION_PUSH = false;

    /**
     * File transfer thumbnail capability
     */
    public static final String CAPABILITY_FILE_TRANSFER_THUMBNAIL = "CapabilityFileTransferThumbnail";
    /* package private */static final Boolean DEFAULT_CAPABILITY_FT_THUMBNAIL = false;

    /**
     * File transfer Store & Forward
     */
    public static final String CAPABILITY_FILE_TRANSFER_SF = "CapabilityFileTransferSF";
    /* package private */static final Boolean DEFAULT_CAPABILITY_FT_SF = false;

    /**
     * Group chat Store & Forward
     */
    public static final String CAPABILITY_GROUP_CHAT_SF = "CapabilityGroupChatSF";
    /* package private */static final Boolean DEFAULT_CAPABILITY_GC_SF = false;

    /**
     * RCS extensions capability
     */
    public static final String CAPABILITY_RCS_EXTENSIONS = "CapabilityRcsExtensions";
    /* package private */static final String DEFAULT_CAPABILITY_RCS_EXTENSIONS = null;

    /**
     * Call composer
     */
    public static final String CAPABILITY_CALL_COMPOSER = "CapabilityCallComposer";
    /* package private */static final Boolean DEFAULT_CAPABILITY_CALL_COMPOSER = true;

    /**
     * Shared map
     */
    public static final String CAPABILITY_SHARED_MAP = "CapabilitySharedMap";
    /* package private */static final Boolean DEFAULT_CAPABILITY_SHARED_MAP = true;

    /**
     * Shared sketch
     */
    public static final String CAPABILITY_SHARED_SKETCH = "CapabilitySharedSketch";
    /* package private */static final Boolean DEFAULT_CAPABILITY_SHARED_SKETCH = true;

    /**
     * Post call
     */
    public static final String CAPABILITY_POST_CALL = "CapabilityPostCall";
    /* package private */static final Boolean DEFAULT_CAPABILITY_POST_CALL = true;

    /**
     * Instant messaging is always on (Store & Forward server)
     */
    public static final String IM_CAPABILITY_ALWAYS_ON = "ImAlwaysOn";
    /* package private */static final Boolean DEFAULT_IM_CAPABILITY_ALWAYS_ON = true;

    /**
     * Group chat Store & Forward to invite participants
     */
    public static final String GROUP_CHAT_INVITE_ONLY_FULL_SF = "GroupChatInviteOnlyFSF";
    /* package private */static final Boolean DEFAULT_GC_INVITE_ONLY_FULL_SF = false;

    /**
     * SIP Automata capability (@see RFC3840)
     */
    public static final String CAPABILITY_SIP_AUTOMATA = "CapabilitySipAutomata";
    /* package private */static final Boolean DEFAULT_CAPABILITY_SIP_AUTOMATA = false;

    /**
     * File transfer always on (Store & Forward server)
     */
    public static final String FT_CAPABILITY_ALWAYS_ON = "FtAlwaysOn";
    /* package private */static final Boolean DEFAULT_FT_CAPABILITY_ALWAYS_ON = false;

    /**
     * File transfer over HTTP always on
     */
    public static final String FT_HTTP_CAP_ALWAYS_ON = "ftHTTPCapAlwaysOn";
    /* package private */static final Boolean DEFAULT_FT_HTTP_CAP_ALWAYS_ON = false;

    public static final String MSG_DELIVERY_TIMEOUT = "msgDeliveryTimeout";
    /*
     * According to joyn Blackbird Product Definition Document Delivery Timeout - "This parameter
     * controls the timeout for the reception of Optional delivery reports for joyn messages after
     * which a capability Parameter check is done to verify whether the contact is offline. When set
     * to 0 the timeout shall not be used as a trigger for the capability exchange. A default value
     * of 300 seconds is used in case the parameter is not provided."
     */
    /* package private */static final Long DEFAULT_MSG_DELIVERY_TIMEOUT = 300000L;

    /**
     * Contacts validity period in one-one messaging
     */
    public static final String MSG_CAP_VALIDITY_PERIOD = "msgCapValidity";
    /* package private */static final Long DEFAULT_MSG_CAP_VALIDITY_PERIOD = 0L;

    /**
     * Instant messaging use report
     */
    public static final String IM_USE_REPORTS = "ImUseReports";
    /* package private */static final Boolean DEFAULT_IM_USE_REPORTS = true;

    /**
     * Network access authorized
     */
    public static final String NETWORK_ACCESS = "NetworkAccess";
    /* package private */static final Integer DEFAULT_NETWORK_ACCESS = NetworkAccessType.ANY
            .toInt();

    /**
     * SIP stack timer T1 in milliseconds
     */
    public static final String SIP_TIMER_T1 = "SipTimerT1";
    /* package private */static final Long DEFAULT_SIP_TIMER_T1 = 2000L;

    /**
     * SIP stack timer T2 in milliseconds
     */
    public static final String SIP_TIMER_T2 = "SipTimerT2";
    /* package private */static final Long DEFAULT_SIP_TIMER_T2 = 16000L;

    /**
     * SIP stack timer T4 in milliseconds
     */
    public static final String SIP_TIMER_T4 = "SipTimerT4";
    /* package private */static final Long DEFAULT_SIP_TIMER_T4 = 17000L;

    /**
     * Enable SIP keep alive
     */
    public static final String SIP_KEEP_ALIVE = "SipKeepAlive";
    /* package private */static final Boolean DEFAULT_SIP_KEEP_ALIVE = true;

    /**
     * SIP keep alive period
     */
    public static final String SIP_KEEP_ALIVE_PERIOD = "SipKeepAlivePeriod";
    /* package private */static final Long DEFAULT_SIP_KEEP_ALIVE_PERIOD = 60000L;

    /**
     * RCS APN
     */
    public static final String RCS_APN = "RcsApn";
    /* package private */static final String DEFAULT_RCS_APN = null;

    /**
     * RCS operator
     */
    public static final String RCS_OPERATOR = "RcsOperator";
    /* package private */static final String DEFAULT_RCS_OPERATOR = null;

    /**
     * GRUU support
     */
    public static final String GRUU = "GRUU";
    /* package private */static final Boolean DEFAULT_GRUU = true;

    /**
     * IMEI used as device ID
     */
    public static final String USE_IMEI_AS_DEVICE_ID = "ImeiDeviceId";
    /* package private */static final Boolean DEFAULT_USE_IMEI_AS_DEVICE_ID = true;

    /**
     * CPU always_on support
     */
    public static final String CPU_ALWAYS_ON = "CpuAlwaysOn";
    /* package private */static final Boolean DEFAULT_CPU_ALWAYS_ON = false;

    /**
     * Configuration mode
     */
    public static final String CONFIG_MODE = "ConfigMode";
    /* package private */static final Integer DEFAULT_CONFIG_MODE = ConfigurationMode.AUTO.toInt();

    /**
     * Terms and conditions response
     */
    /* package private */static final String TC_RESPONSE = "TCResp";
    /* package private */static final Integer DEFAULT_TC_RESPONSE = TermsAndConditionsResponse.NO_ANSWER
            .toInt();

    /**
     * Provisioning version
     */
    public static final String PROVISIONING_VERSION = "ProvisioningVersion";
    /* package private */static final String DEFAULT_PROVISIONING_VERSION = "0";

    /**
     * Provisioning version
     */
    public static final String PROVISIONING_TOKEN = "ProvisioningToken";
    /* package private */static final String DEFAULT_PROVISIONING_TOKEN = null;

    /**
     * Secondary provisioning address
     */
    public static final String SECONDARY_PROVISIONING_ADDRESS = "SecondaryProvisioningAddress";
    /* package private */static final String DEFAULT_SECONDARY_PROV_ADDR = null;

    /**
     * Use only the secondary provisioning address
     */
    public static final String SECONDARY_PROVISIONING_ADDRESS_ONLY = "SecondaryProvisioningAddressOnly";
    /* package private */static final Boolean DEFAULT_SECONDARY_PROV_ADDR_ONLY = false;

    /**
     * Directory path for photos
     */
    public static final String DIRECTORY_PATH_PHOTOS = "DirectoryPathPhotos";
    /* package private */static final String DEFAULT_DIRECTORY_PATH_PHOTOS = Environment
            .getExternalStorageDirectory() + "/rcs/photos/";

    /**
     * Directory path for videos
     */
    public static final String DIRECTORY_PATH_VIDEOS = "DirectoryPathVideos";
    /* package private */static final String DEFAULT_DIRECTORY_PATH_VIDEOS = Environment
            .getExternalStorageDirectory() + "/rcs/videos/";

    /**
     * Directory path for audios
     */
    public static final String DIRECTORY_PATH_AUDIOS = "DirectoryPathAudios";
    /* package private */static final String DEFAULT_DIRECTORY_PATH_AUDIOS = Environment
            .getExternalStorageDirectory() + "/rcs/audios/";

    /**
     * Directory path for files
     */
    public static final String DIRECTORY_PATH_FILES = "DirectoryPathFiles";
    /* package private */static final String DEFAULT_DIRECTORY_PATH_FILES = Environment
            .getExternalStorageDirectory() + "/rcs/files/";

    /**
     * Directory path for file icons
     */
    public static final String DIRECTORY_PATH_FILEICONS = "DirectoryPathFileIcons";
    /* package private */static final String DEFAULT_DIRECTORY_PATH_FILEICONS = Environment
            .getExternalStorageDirectory() + "/rcs/fileicons/";

    /**
     * Secure MSRP over Mobile access
     */
    public static final String SECURE_MSRP_OVER_MOBILE = "SecureMsrpOverMobile";
    /* package private */static final Boolean DEFAULT_SECURE_MSRP_OVER_MOBILE = false;

    /**
     * Secure RTP over Mobile access
     */
    public static final String SECURE_RTP_OVER_MOBILE = "SecureRtpOverMobile";
    /* package private */static final Boolean DEFAULT_SECURE_RTP_OVER_MOBILE = false;

    /**
     * Secure MSRP over Wi-Fi
     */
    public static final String SECURE_MSRP_OVER_WIFI = "SecureMsrpOverWifi";
    /* package private */static final Boolean DEFAULT_SECURE_MSRP_OVER_WIFI = false;

    /**
     * Secured RTP over Wi-Fi
     */
    public static final String SECURE_RTP_OVER_WIFI = "SecureRtpOverWifi";
    /* package private */static final Boolean DEFAULT_SECURE_RTP_OVER_WIFI = false;

    /**
     * Key and associated values for GSMA release of the device as provisioned by the network
     */
    public static final String KEY_GSMA_RELEASE = "GsmaRelease";
    /* package private */static final Integer DEFAULT_KEY_GSMA_RELEASE = GsmaRelease.BLACKBIRD
            .toInt();

    /**
     * IP voice call breakout capabilities in RCS-AA mode
     */
    public static final String IPVOICECALL_BREAKOUT_AA = "IPCallBreakOutAA";
    /* package private */static final Boolean DEFAULT_IPVOICECALL_BREAKOUT_AA = false;

    /**
     * IP voice call breakout capabilities in RCS-CS mode
     */
    public static final String IPVOICECALL_BREAKOUT_CS = "IPCallBreakOutCS";
    /* package private */static final Boolean DEFAULT_IPVOICECALL_BREAKOUT_CS = false;

    /**
     * CS call upgrade to IP Video Call in RCS-CS mode
     */
    public static final String IPVIDEOCALL_UPGRADE_FROM_CS = "rcsIPVideoCallUpgradeFromCS";
    /* package private */static final Boolean DEFAULT_IPVIDEOCALL_UPGRADE_FROM_CS = false;

    /**
     * CS call upgrade to IP Video Call in case of capability error
     */
    public static final String IPVIDEOCALL_UPGRADE_ON_CAPERROR = "rcsIPVideoCallUpgradeOnCapError";
    /* package private */static final Boolean DEFAULT_IPVIDEOCALL_UPGRADE_ON_CAPERROR = false;

    /**
     * Leaf node that tells an RCS-CS device whether it can initiate an RCS IP Video Call upgrade
     * without first tearing down the CS voice call
     */
    public static final String IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY = "rcsIPVideoCallUpgradeAttemptEarly";
    /* package private */static final Boolean DEFAULT_IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY = false;

    /**
     * TCP fallback option
     */
    public static final String TCP_FALLBACK = "TcpFallback";
    /* package private */static final Boolean DEFAULT_TCP_FALLBACK = false;

    /**
     * Control RCS extensions
     */
    public static final String CONTROL_EXTENSIONS = "ControlRcsExtensions";
    /* package private */static final Boolean DEFAULT_CONTROL_EXTENSIONS = false;

    /**
     * Allow RCS extensions
     */
    public static final String ALLOW_EXTENSIONS = "AllowRcsExtensions";
    /* package private */static final Boolean DEFAULT_ALLOW_EXTENSIONS = true;

    /**
     * Max length for extensions using real time messaging (MSRP)
     */
    public static final String MAX_MSRP_SIZE_EXTENSIONS = "ExtensionsMaxMsrpSize";
    /* package private */static final Integer DEFAULT_MAX_MSRP_SIZE_EXTENSIONS = 0;

    /**
     * Call composer inactivity timeout before session is closed
     */
    public static final String CALL_COMPOSER_INACTIVITY_TIMEOUT = "CallComposerInactivityTimeout";
    /* package private */static final Long DEFAULT_CALL_COMPOSER_INACTIVITY_TIMEOUT = 180000L;

    /**
     * Validity of the RCS configuration.
     */
    public static final String CONFIGURATION_VALID = "ConfigurationValidity";
    /* package private */static final Boolean DEFAULT_CONFIGURATION_VALID = false;

    /**
     * Auto accept file transfer invitation in roaming
     */
    public static final String AUTO_ACCEPT_FT_IN_ROAMING = "AutoAcceptFtInRoaming";
    /* package private */static final Boolean DEFAULT_AUTO_ACCEPT_FT_IN_ROAMING = false;

    /**
     * Auto accept file transfer enabled
     */
    public static final String AUTO_ACCEPT_FT_CHANGEABLE = "AutoAcceptFtChangeable";
    /* package private */static final Boolean DEFAULT_AUTO_ACCEPT_FT_CHANGEABLE = false;

    /**
     * Image resize option
     */
    public static final String KEY_IMAGE_RESIZE_OPTION = "ImageResizeOption";
    /* package private */static final Integer DEFAULT_KEY_IMAGE_RESIZE_OPTION = ImageResizeOption.ALWAYS_ASK
            .toInt();

    /**
     * RCS stack can be activated/deactivated by client applications
     */
    public static final String ENABLE_RCS_SWITCH = "enableRcseSwitch";
    /* package private */static final Integer DEFAULT_ENABLE_RCS_SWITCH = EnableRcseSwitch.ALWAYS_SHOW
            .toInt();

    public static final String IM_MSG_TECH = "ImMsgTech";
    /* package private */static final Integer DEFAULT_IM_MSG_TECH = ImMsgTech.SIMPLE_IM.toInt();

    public static final String FIRST_MESSAGE_INVITE = "FirstMessageInvite";
    /* package private */static final Boolean DEFAULT_FIRST_MESSAGE_INVITE = true;

    public static final String REQUEST_AND_RESPOND_TO_GROUP_DISPLAY_REPORTS = "RequestAndRespondToGroupDisplayReports";
    /* package private */static final Boolean DEFAULT_REQUEST_AND_RESPOND_TO_GROUP_DISPLAY_REPORTS = false;

    public static final String MAX_ALLOWED_DISPLAY_NAME_CHARS = "MaxAllowedDisplayNameChars";
    /* package private */static final Integer DEFAULT_MAX_ALLOWED_DISPLAY_NAME_CHARS = 256;

    /**
     * Provisioning optional user message content associated with the result of the configuration
     * server response
     */
    /* package private */static final String PROV_USER_MSG_CONTENT = "message";
    /* package private */static final String DEFAULT_PROV_USER_MSG_CONTENT = null;

    /**
     * Provisioning optional user message title associated with the result of the configuration
     * server response
     */
    /* package private */static final String PROV_USER_MSG_TITLE = "title";
    /* package private */static final String DEFAULT_PROV_USER_MSG_TITLE = null;

    /**
     * Mobile Country Code (0 if undefined)
     */
    /* package private */static final String MOBILE_COUNTRY_CODE = "mcc";
    /* package private */static final Integer DEFAULT_MOBILE_COUNTRY_CODE = 0;

    /**
     * Mobile Network Code (0 if undefined)
     */
    /* package private */static final String MOBILE_NETWORK_CODE = "mnc";
    /* package private */static final Integer DEFAULT_MOBILE_NETWORK_CODE = 0;

    /* package private */static final String PROV_ACCEPT_BUTTON = "Accept_btn";
    /* package private */static final Boolean DEFAULT_PROV_ACCEPT_BUTTON = false;

    /* package private */static final String PROV_REJECT_BUTTON = "Reject_btn";
    /* package private */static final Boolean DEFAULT_PROV_REJECT_BUTTON = false;

    /* package private */static final String LOCAL_DISPLAY_LANGUAGE = "language";
    /* package private */static final String DEFAULT_LOCAL_DISPLAY_LANGUAGE = null;

    /* package private */final static Map<String, Object> sSettingsKeyDefaultValue;
    static {
        sSettingsKeyDefaultValue = new HashMap<>();
        sSettingsKeyDefaultValue.put(RcsSettingsData.SERVICE_ACTIVATED,
                RcsSettingsData.DEFAULT_SERVICE_ACTIVATED);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CHAT_RESPOND_TO_DISPLAY_REPORTS,
                RcsSettingsData.DEFAULT_CHAT_RESPOND_TO_DISPLAY_REPORTS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MIN_BATTERY_LEVEL,
                RcsSettingsData.DEFAULT_MIN_BATTERY_LEVEL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_FILE_ICON_SIZE,
                RcsSettingsData.DEFAULT_MAX_FILE_ICON_SIZE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_PHOTO_ICON_SIZE,
                RcsSettingsData.DEFAULT_MAX_PHOTO_ICON_SIZE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_FREETXT_LENGTH,
                RcsSettingsData.DEFAULT_MAX_FREETXT_LENGTH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_GEOLOC_LABEL_LENGTH,
                RcsSettingsData.DEFAULT_MAX_GEOLOC_LABEL_LENGTH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.GEOLOC_EXPIRATION_TIME,
                RcsSettingsData.DEFAULT_GEOLOC_EXPIRATION_TIME);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_CHAT_PARTICIPANTS,
                RcsSettingsData.DEFAULT_MAX_CHAT_PARTICIPANTS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_STANDALONE_MSG_LENGTH,
                RcsSettingsData.DEFAULT_MAX_STANDALONE_MSG_LENGTH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_CHAT_MSG_LENGTH,
                RcsSettingsData.DEFAULT_MAX_CHAT_MSG_LENGTH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_GROUPCHAT_MSG_LENGTH,
                RcsSettingsData.DEFAULT_MAX_GC_MSG_LENGTH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CHAT_IDLE_DURATION,
                RcsSettingsData.DEFAULT_CHAT_IDLE_DURATION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_FILE_TRANSFER_SIZE,
                RcsSettingsData.DEFAULT_MAX_FT_SIZE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.WARN_FILE_TRANSFER_SIZE,
                RcsSettingsData.DEFAULT_WARN_FT_SIZE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_IMAGE_SHARE_SIZE,
                RcsSettingsData.DEFAULT_MAX_ISH_SIZE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_VIDEO_SHARE_DURATION,
                RcsSettingsData.DEFAULT_MAX_VSH_DURATION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_AUDIO_MESSAGE_DURATION,
                RcsSettingsData.DEFAULT_MAX_AUDIO_DURATION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_CHAT_SESSIONS,
                RcsSettingsData.DEFAULT_MAX_CHAT_SESSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_FILE_TRANSFER_SESSIONS,
                RcsSettingsData.DEFAULT_MAX_FT_SESSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_CONCURRENT_OUTGOING_FILE_TRANSFERS,
                RcsSettingsData.DEFAULT_MAX_CONCURRENT_OUTGOING_FT_SESSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_IP_CALL_SESSIONS,
                RcsSettingsData.DEFAULT_MAX_IP_CALL_SESSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SMS_FALLBACK_SERVICE,
                RcsSettingsData.DEFAULT_SMS_FALLBACK_SERVICE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.WARN_SF_SERVICE,
                RcsSettingsData.DEFAULT_WARN_SF_SERVICE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.AUTO_ACCEPT_CHAT,
                RcsSettingsData.DEFAULT_AUTO_ACCEPT_CHAT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.AUTO_ACCEPT_GROUP_CHAT,
                RcsSettingsData.DEFAULT_AUTO_ACCEPT_GC);
        sSettingsKeyDefaultValue.put(RcsSettingsData.AUTO_ACCEPT_FILE_TRANSFER,
                RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IM_SESSION_START,
                RcsSettingsData.DEFAULT_IM_SESSION_START);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_USERNAME,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_USERNAME);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_DISPLAY_NAME);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_HOME_DOMAIN);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_PUBLIC_ID_PC,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_PUBLIC_ID_PC);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_PRIVATE_ID);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_PASSWORD,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_PASSWORD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USERPROFILE_IMS_REALM,
                RcsSettingsData.DEFAULT_USERPROFILE_IMS_REALM);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_PROXY_ADDR_MOBILE,
                RcsSettingsData.DEFAULT_IMS_PROXY_ADDR_MOBILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_PROXY_PORT_MOBILE,
                RcsSettingsData.DEFAULT_IMS_PROXY_PORT_MOBILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_PROXY_ADDR_WIFI,
                RcsSettingsData.DEFAULT_IMS_PROXY_ADDR_WIFI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_PROXY_PORT_WIFI,
                RcsSettingsData.DEFAULT_IMS_PROXY_PORT_WIFI);
        sSettingsKeyDefaultValue
                .put(RcsSettingsData.XDM_SERVER, RcsSettingsData.DEFAULT_XDM_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.XDM_LOGIN, RcsSettingsData.DEFAULT_XDM_LOGIN);
        sSettingsKeyDefaultValue.put(RcsSettingsData.XDM_PASSWORD,
                RcsSettingsData.DEFAULT_XDM_PASSWORD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FT_HTTP_SERVER,
                RcsSettingsData.DEFAULT_FT_HTTP_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FT_HTTP_LOGIN,
                RcsSettingsData.DEFAULT_FT_HTTP_LOGIN);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FT_HTTP_PASSWORD,
                RcsSettingsData.DEFAULT_FT_HTTP_PASSWORD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FT_PROTOCOL,
                RcsSettingsData.DEFAULT_FT_PROTOCOL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IM_MASS_URI,
                RcsSettingsData.DEFAULT_IM_MASS_URI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IM_CONF_URI,
                RcsSettingsData.DEFAULT_IM_CONF_URI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.ENDUSER_CONFIRMATION_URI,
                RcsSettingsData.DEFAULT_ENDUSER_CONFIRMATION_URI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MSG_STORE_SERVER,
                RcsSettingsData.DEFAULT_MSG_STORE_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROFILE_SERVER,
                RcsSettingsData.DEFAULT_PROFILE_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PUBLICACCOUNT_SERVER,
                RcsSettingsData.DEFAULT_PUBLICACCOUNT_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SSO_SERVER,
                RcsSettingsData.DEFAULT_SSO_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.QRCARD_SERVER,
                RcsSettingsData.DEFAULT_QRCARD_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PC_APPLICATION_SERVER,
                RcsSettingsData.DEFAULT_PC_APPLICATION_SERVER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.UUID, RcsSettingsData.DEFAULT_UUID);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_CS_VIDEO,
                RcsSettingsData.DEFAULT_CAPABILITY_CS_VIDEO);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_IMAGE_SHARING,
                RcsSettingsData.DEFAULT_CAPABILITY_ISH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_VIDEO_SHARING,
                RcsSettingsData.DEFAULT_CAPABILITY_VSH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_IP_VOICE_CALL,
                RcsSettingsData.DEFAULT_CAPABILITY_IP_VOICE_CALL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_IP_VIDEO_CALL,
                RcsSettingsData.DEFAULT_CAPABILITY_IP_VIDEO_CALL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_STANDALONE_MESSAGING,
                RcsSettingsData.DEFAULT_CAPABILITY_STANDALONE_MESSAGING);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_IM_SESSION,
                RcsSettingsData.DEFAULT_CAPABILITY_IM_SESSION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_IM_GROUP_SESSION,
                RcsSettingsData.DEFAULT_CAPABILITY_IM_GROUP_SESSION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_FILE_TRANSFER,
                RcsSettingsData.DEFAULT_CAPABILITY_FT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,
                RcsSettingsData.DEFAULT_CAPABILITY_FT_HTTP);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                RcsSettingsData.DEFAULT_CAPABILITY_PRESENCE_DISCOVERY);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE,
                RcsSettingsData.DEFAULT_CAPABILITY_SOCIAL_PRESENCE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
                RcsSettingsData.DEFAULT_CAPABILITY_GEOLOCATION_PUSH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL,
                RcsSettingsData.DEFAULT_CAPABILITY_FT_THUMBNAIL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_GROUP_CHAT_SF,
                RcsSettingsData.DEFAULT_CAPABILITY_GC_SF);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF,
                RcsSettingsData.DEFAULT_CAPABILITY_FT_SF);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_RCS_EXTENSIONS,
                RcsSettingsData.DEFAULT_CAPABILITY_RCS_EXTENSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_CALL_COMPOSER,
                RcsSettingsData.DEFAULT_CAPABILITY_CALL_COMPOSER);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_SHARED_MAP,
                RcsSettingsData.DEFAULT_CAPABILITY_SHARED_MAP);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_SHARED_SKETCH,
                RcsSettingsData.DEFAULT_CAPABILITY_SHARED_SKETCH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_POST_CALL,
                RcsSettingsData.DEFAULT_CAPABILITY_POST_CALL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_SERVICE_POLLING_PERIOD,
                RcsSettingsData.DEFAULT_IMS_SERVICE_POLLING_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_DEFAULT_PORT,
                RcsSettingsData.DEFAULT_SIP_DEFAULT_PORT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_MOBILE,
                RcsSettingsData.DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_MOBILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_DEFAULT_PROTOCOL_FOR_WIFI,
                RcsSettingsData.DEFAULT_SIP_DEFAULT_PROTOCOL_FOR_WIFI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TLS_CERTIFICATE_ROOT,
                RcsSettingsData.DEFAULT_TLS_CERTIFICATE_ROOT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TLS_CERTIFICATE_INTERMEDIATE,
                RcsSettingsData.DEFAULT_TLS_CERTIFICATE_INTERMEDIATE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_TRANSACTION_TIMEOUT,
                RcsSettingsData.DEFAULT_SIP_TRANSACTION_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MSRP_DEFAULT_PORT,
                RcsSettingsData.DEFAULT_MSRP_DEFAULT_PORT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.RTP_DEFAULT_PORT,
                RcsSettingsData.DEFAULT_RTP_DEFAULT_PORT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MSRP_TRANSACTION_TIMEOUT,
                RcsSettingsData.DEFAULT_MSRP_TRANSACTION_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.REGISTER_EXPIRE_PERIOD,
                RcsSettingsData.DEFAULT_REGISTER_EXPIRE_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.REGISTER_RETRY_BASE_TIME,
                RcsSettingsData.DEFAULT_REGISTER_RETRY_BASE_TIME);
        sSettingsKeyDefaultValue.put(RcsSettingsData.REGISTER_RETRY_MAX_TIME,
                RcsSettingsData.DEFAULT_REGISTER_RETRY_MAX_TIME);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PUBLISH_EXPIRE_PERIOD,
                RcsSettingsData.DEFAULT_PUBLISH_EXPIRE_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.REVOKE_TIMEOUT,
                RcsSettingsData.DEFAULT_REVOKE_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                RcsSettingsData.DEFAULT_IMS_AUTHENT_PROCEDURE_MOBILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI,
                RcsSettingsData.DEFAULT_IMS_AUTHENT_PROCEDURE_WIFI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TEL_URI_FORMAT,
                RcsSettingsData.DEFAULT_TEL_URI_FORMAT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.RINGING_SESSION_PERIOD,
                RcsSettingsData.DEFAULT_RINGING_SESSION_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SUBSCRIBE_EXPIRE_PERIOD,
                RcsSettingsData.DEFAULT_SUBSCRIBE_EXPIRE_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IS_COMPOSING_TIMEOUT,
                RcsSettingsData.DEFAULT_IS_COMPOSING_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SESSION_REFRESH_EXPIRE_PERIOD,
                RcsSettingsData.DEFAULT_SESSION_REFRESH_EXPIRE_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PERMANENT_STATE_MODE,
                RcsSettingsData.DEFAULT_PERMANENT_STATE_MODE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TRACE_ACTIVATED,
                RcsSettingsData.DEFAULT_TRACE_ACTIVATED);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TRACE_LEVEL,
                RcsSettingsData.DEFAULT_TRACE_LEVEL);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_TRACE_ACTIVATED,
                RcsSettingsData.DEFAULT_SIP_TRACE_ACTIVATED);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_TRACE_FILE,
                RcsSettingsData.DEFAULT_SIP_TRACE_FILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MEDIA_TRACE_ACTIVATED,
                RcsSettingsData.DEFAULT_MEDIA_TRACE_ACTIVATED);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_REFRESH_TIMEOUT,
                RcsSettingsData.DEFAULT_CAPABILITY_REFRESH_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_EXPIRY_TIMEOUT,
                RcsSettingsData.DEFAULT_CAPABILITY_EXPIRY_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_POLLING_PERIOD,
                RcsSettingsData.DEFAULT_CAPABILITY_POLLING_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IM_CAPABILITY_ALWAYS_ON,
                RcsSettingsData.DEFAULT_IM_CAPABILITY_ALWAYS_ON);
        sSettingsKeyDefaultValue.put(RcsSettingsData.GROUP_CHAT_INVITE_ONLY_FULL_SF,
                RcsSettingsData.DEFAULT_GC_INVITE_ONLY_FULL_SF);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FT_CAPABILITY_ALWAYS_ON,
                RcsSettingsData.DEFAULT_FT_CAPABILITY_ALWAYS_ON);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FT_HTTP_CAP_ALWAYS_ON,
                RcsSettingsData.DEFAULT_FT_HTTP_CAP_ALWAYS_ON);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MSG_DELIVERY_TIMEOUT,
                RcsSettingsData.DEFAULT_MSG_DELIVERY_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MSG_CAP_VALIDITY_PERIOD,
                RcsSettingsData.DEFAULT_MSG_CAP_VALIDITY_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IM_USE_REPORTS,
                RcsSettingsData.DEFAULT_IM_USE_REPORTS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.NETWORK_ACCESS,
                RcsSettingsData.DEFAULT_NETWORK_ACCESS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_TIMER_T1,
                RcsSettingsData.DEFAULT_SIP_TIMER_T1);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_TIMER_T2,
                RcsSettingsData.DEFAULT_SIP_TIMER_T2);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_TIMER_T4,
                RcsSettingsData.DEFAULT_SIP_TIMER_T4);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_KEEP_ALIVE,
                RcsSettingsData.DEFAULT_SIP_KEEP_ALIVE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SIP_KEEP_ALIVE_PERIOD,
                RcsSettingsData.DEFAULT_SIP_KEEP_ALIVE_PERIOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.RCS_APN, RcsSettingsData.DEFAULT_RCS_APN);
        sSettingsKeyDefaultValue.put(RcsSettingsData.RCS_OPERATOR,
                RcsSettingsData.DEFAULT_RCS_OPERATOR);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_CHAT_LOG_ENTRIES,
                RcsSettingsData.DEFAULT_MAX_CHAT_LOG_ENTRIES);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_RICHCALL_LOG_ENTRIES,
                RcsSettingsData.DEFAULT_MAX_RICHCALL_LOG_ENTRIES);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_IPCALL_LOG_ENTRIES,
                RcsSettingsData.DEFAULT_MAX_IPCALL_LOG_ENTRIES);
        sSettingsKeyDefaultValue.put(RcsSettingsData.GRUU, RcsSettingsData.DEFAULT_GRUU);
        sSettingsKeyDefaultValue.put(RcsSettingsData.USE_IMEI_AS_DEVICE_ID,
                RcsSettingsData.DEFAULT_USE_IMEI_AS_DEVICE_ID);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CPU_ALWAYS_ON,
                RcsSettingsData.DEFAULT_CPU_ALWAYS_ON);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CONFIG_MODE,
                RcsSettingsData.DEFAULT_CONFIG_MODE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TC_RESPONSE,
                RcsSettingsData.DEFAULT_TC_RESPONSE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROVISIONING_VERSION,
                RcsSettingsData.DEFAULT_PROVISIONING_VERSION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROVISIONING_TOKEN,
                RcsSettingsData.DEFAULT_PROVISIONING_TOKEN);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS,
                RcsSettingsData.DEFAULT_SECONDARY_PROV_ADDR);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SECONDARY_PROVISIONING_ADDRESS_ONLY,
                RcsSettingsData.DEFAULT_SECONDARY_PROV_ADDR_ONLY);
        sSettingsKeyDefaultValue.put(RcsSettingsData.DIRECTORY_PATH_PHOTOS,
                RcsSettingsData.DEFAULT_DIRECTORY_PATH_PHOTOS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.DIRECTORY_PATH_VIDEOS,
                RcsSettingsData.DEFAULT_DIRECTORY_PATH_VIDEOS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.DIRECTORY_PATH_AUDIOS,
                RcsSettingsData.DEFAULT_DIRECTORY_PATH_AUDIOS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.DIRECTORY_PATH_FILES,
                RcsSettingsData.DEFAULT_DIRECTORY_PATH_FILES);
        sSettingsKeyDefaultValue.put(RcsSettingsData.DIRECTORY_PATH_FILEICONS,
                RcsSettingsData.DEFAULT_DIRECTORY_PATH_FILEICONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SECURE_MSRP_OVER_MOBILE,
                RcsSettingsData.DEFAULT_SECURE_MSRP_OVER_MOBILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SECURE_RTP_OVER_MOBILE,
                RcsSettingsData.DEFAULT_SECURE_RTP_OVER_MOBILE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SECURE_MSRP_OVER_WIFI,
                RcsSettingsData.DEFAULT_SECURE_MSRP_OVER_WIFI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.SECURE_RTP_OVER_WIFI,
                RcsSettingsData.DEFAULT_SECURE_RTP_OVER_WIFI);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MESSAGING_MODE,
                RcsSettingsData.DEFAULT_KEY_MESSAGING_MODE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CAPABILITY_SIP_AUTOMATA,
                RcsSettingsData.DEFAULT_CAPABILITY_SIP_AUTOMATA);
        sSettingsKeyDefaultValue.put(RcsSettingsData.KEY_GSMA_RELEASE,
                RcsSettingsData.DEFAULT_KEY_GSMA_RELEASE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IPVOICECALL_BREAKOUT_AA,
                RcsSettingsData.DEFAULT_IPVOICECALL_BREAKOUT_AA);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IPVOICECALL_BREAKOUT_CS,
                RcsSettingsData.DEFAULT_IPVOICECALL_BREAKOUT_CS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IPVIDEOCALL_UPGRADE_FROM_CS,
                RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_FROM_CS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IPVIDEOCALL_UPGRADE_ON_CAPERROR,
                RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_ON_CAPERROR);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY,
                RcsSettingsData.DEFAULT_IPVIDEOCALL_UPGRADE_ATTEMPT_EARLY);
        sSettingsKeyDefaultValue.put(RcsSettingsData.TCP_FALLBACK,
                RcsSettingsData.DEFAULT_TCP_FALLBACK);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CONTROL_EXTENSIONS,
                RcsSettingsData.DEFAULT_CONTROL_EXTENSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.ALLOW_EXTENSIONS,
                RcsSettingsData.DEFAULT_ALLOW_EXTENSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_MSRP_SIZE_EXTENSIONS,
                RcsSettingsData.DEFAULT_MAX_MSRP_SIZE_EXTENSIONS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CALL_COMPOSER_INACTIVITY_TIMEOUT,
                RcsSettingsData.DEFAULT_CALL_COMPOSER_INACTIVITY_TIMEOUT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.CONFIGURATION_VALID,
                RcsSettingsData.DEFAULT_CONFIGURATION_VALID);
        sSettingsKeyDefaultValue.put(RcsSettingsData.AUTO_ACCEPT_FT_IN_ROAMING,
                RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT_IN_ROAMING);
        sSettingsKeyDefaultValue.put(RcsSettingsData.AUTO_ACCEPT_FT_CHANGEABLE,
                RcsSettingsData.DEFAULT_AUTO_ACCEPT_FT_CHANGEABLE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.DEFAULT_MESSAGING_METHOD,
                RcsSettingsData.DEFAULT_KEY_DEFAULT_MESSAGING_METHOD);
        sSettingsKeyDefaultValue.put(RcsSettingsData.KEY_IMAGE_RESIZE_OPTION,
                RcsSettingsData.DEFAULT_KEY_IMAGE_RESIZE_OPTION);
        sSettingsKeyDefaultValue.put(RcsSettingsData.ENABLE_RCS_SWITCH,
                RcsSettingsData.DEFAULT_ENABLE_RCS_SWITCH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.IM_MSG_TECH,
                RcsSettingsData.DEFAULT_IM_MSG_TECH);
        sSettingsKeyDefaultValue.put(RcsSettingsData.FIRST_MESSAGE_INVITE,
                RcsSettingsData.DEFAULT_FIRST_MESSAGE_INVITE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.REQUEST_AND_RESPOND_TO_GROUP_DISPLAY_REPORTS,
                RcsSettingsData.DEFAULT_REQUEST_AND_RESPOND_TO_GROUP_DISPLAY_REPORTS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MAX_ALLOWED_DISPLAY_NAME_CHARS,
                RcsSettingsData.DEFAULT_MAX_ALLOWED_DISPLAY_NAME_CHARS);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROV_USER_MSG_CONTENT,
                RcsSettingsData.DEFAULT_PROV_USER_MSG_CONTENT);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROV_USER_MSG_TITLE,
                RcsSettingsData.DEFAULT_PROV_USER_MSG_TITLE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MOBILE_COUNTRY_CODE,
                RcsSettingsData.DEFAULT_MOBILE_COUNTRY_CODE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.MOBILE_NETWORK_CODE,
                RcsSettingsData.DEFAULT_MOBILE_NETWORK_CODE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROV_ACCEPT_BUTTON,
                RcsSettingsData.DEFAULT_PROV_ACCEPT_BUTTON);
        sSettingsKeyDefaultValue.put(RcsSettingsData.PROV_REJECT_BUTTON,
                RcsSettingsData.DEFAULT_PROV_REJECT_BUTTON);
        sSettingsKeyDefaultValue.put(RcsSettingsData.LOCAL_DISPLAY_LANGUAGE,
                RcsSettingsData.DEFAULT_LOCAL_DISPLAY_LANGUAGE);
        sSettingsKeyDefaultValue.put(RcsSettingsData.ENRICH_CALLING_SERVICE,
                RcsSettingsData.DEFAULT_ENRICH_CALLING_SERVICE);
    }
}
