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

package com.gsma.services.rcs.chat;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.StringTokenizer;

/**
 * Emoticon class
 */
public class Emoticon implements Parcelable, Serializable {

    /**
     * <?xml version="1.0"encoding="UTF-8"?> <vemoticon
     * xmlns="http://vemoticon.bj.ims.mnc000.mcc460.3gppnetwork.org/types"> <sms>smile</sms>
     * <eid>E55A257E5B93CE76AC0F3DE43A3C284D</eid> </vemoticon >
     */

    private final String mSms;

    private final String mEid;

    /**
     * Constructor
     *
     * @param sms sms
     * @param eid eid
     */
    public Emoticon(String sms, String eid) {
        mSms = sms;
        mEid = eid;
    }

    /**
     * Constructor: returns a Vemoticon instance as parsed from the CONTENT field of a
     * VemoticonMessage in the ChatLog.Message provider.
     *
     * @param vemoticon Provider vemoticon format
     */
    public Emoticon(String vemoticon) {
        StringTokenizer items = new StringTokenizer(vemoticon, ",");
        mSms = String.valueOf(items.nextToken());
        mEid = String.valueOf(items.nextToken());
    }

    /**
     * Constructor
     *
     * @param source Parcelable source
     * @hide
     */
    public Emoticon(Parcel source) {
        mSms = source.readString();
        mEid = source.readString();
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable's marshalled
     * representation.
     *
     * @return Integer
     * @hide
     */
    public int describeContents() {
        return 0;
    }

    /**
     * Write parcelable object.
     *
     * @param dest The Parcel in which the object should be written
     * @param flags Additional flags about how the object should be written
     * @hide
     */
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSms);
        dest.writeString(mEid);
    }

    /**
     * Parcelable creator.
     *
     * @hide
     */
    public static final Creator<Emoticon> CREATOR = new Creator<Emoticon>() {
        public Emoticon createFromParcel(Parcel source) {
            return new Emoticon(source);
        }

        public Emoticon[] newArray(int size) {
            return new Emoticon[size];
        }
    };

    /**
     * Returns the sms.
     * 
     * @return Sms
     */
    public String getSms() {
        return mSms;
    }

    /**
     * Returns the eid.
     * 
     * @return Eid
     */
    public String getEid() {
        return mEid;
    }

    /**
     * Returns the vemoticon in provider format.
     * 
     * @return String
     */
    @Override
    public String toString() {
        return new StringBuilder().append(mSms).append(",").append(mEid).toString();
    }
}
