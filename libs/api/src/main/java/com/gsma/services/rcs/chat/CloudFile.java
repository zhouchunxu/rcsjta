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
 * CloudFile class
 */
public class CloudFile implements Parcelable, Serializable {

    /*
     * <?xml version="1.0" encoding="UTF-8"?> <cloudfile xmlns="http://cloudfile.cmcc.com/types">
     * <filename>日程表.xls</filename> <filesize>3625</filesize> //单位：字节
     * <downloadurl>http://abc.com</downloadurl> </cloudfile>
     */

    private final String mFileName;

    private final long mFileSize;

    private final String mDownloadUrl;

    /**
     * Constructor
     *
     * @param filename filename
     * @param filesize filesize
     * @param downloadurl downloadurl
     */
    public CloudFile(String filename, long filesize, String downloadurl) {
        mFileName = filename;
        mFileSize = filesize;
        mDownloadUrl = downloadurl;
    }

    /**
     * Constructor: returns a CloudFile instance as parsed from the CONTENT field of a
     * CloudFileMessage in the ChatLog.Message provider.
     *
     * @param cloudfile Provider vemoticon format
     */
    public CloudFile(String cloudfile) {
        StringTokenizer items = new StringTokenizer(cloudfile, ",");
        mFileName = String.valueOf(items.nextToken());
        mFileSize = Long.valueOf(items.nextToken());
        mDownloadUrl = String.valueOf(items.nextToken());
    }

    /**
     * Constructor
     *
     * @param source Parcelable source
     * @hide
     */
    public CloudFile(Parcel source) {
        mFileName = source.readString();
        mFileSize = source.readLong();
        mDownloadUrl = source.readString();
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
        dest.writeString(mFileName);
        dest.writeLong(mFileSize);
        dest.writeString(mDownloadUrl);
    }

    /**
     * Parcelable creator.
     *
     * @hide
     */
    public static final Creator<CloudFile> CREATOR = new Creator<CloudFile>() {
        public CloudFile createFromParcel(Parcel source) {
            return new CloudFile(source);
        }

        public CloudFile[] newArray(int size) {
            return new CloudFile[size];
        }
    };

    /**
     * Returns the filename.
     *
     * @return Filename
     */
    public String getFileName() {
        return mFileName;
    }

    /**
     * Returns the filesize.
     *
     * @return Filesize
     */
    public long getFileSize() {
        return mFileSize;
    }

    /**
     * Returns the downloadurl.
     *
     * @return Downloadurl
     */
    public String getDownloadUrl() {
        return mDownloadUrl;
    }

    /**
     * Returns the CloudFile in provider format.
     *
     * @return String
     */
    @Override
    public String toString() {
        return new StringBuilder().append(mFileName).append(",").append(mFileSize).append(",")
                .append(mDownloadUrl).toString();
    }
}
