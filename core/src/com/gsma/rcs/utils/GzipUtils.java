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

package com.gsma.rcs.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Gzip utilities
 */
public class GzipUtils {

    /**
     * Decodes GZIP data into octects
     *
     * @param gzipData Byte array containing GZIP data
     * @return Array containing decoded data.
     */
    public static byte[] decodeGzip(byte[] gzipData) throws IOException {
        BufferedReader reader = null;
        try {
            GZIPInputStream gzipIs = new GZIPInputStream(new ByteArrayInputStream(gzipData));
            if (gzipIs == null) {
                return new byte[0];
            }
            String line;
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(gzipIs, StringUtils.UTF8));
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString().getBytes(StringUtils.UTF8);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
