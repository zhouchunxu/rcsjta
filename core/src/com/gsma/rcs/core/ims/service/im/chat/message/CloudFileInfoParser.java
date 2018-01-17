/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2016 China Mobile.
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

package com.gsma.rcs.core.ims.service.im.chat.message;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.logger.Logger;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

/**
 * CloudFile info parser
 */
public class CloudFileInfoParser extends DefaultHandler {

    /*
     * CloudFile-Info SAMPLE: <?xml version="1.0" encoding="UTF-8"?> <cloudfile
     * xmlns="http://cloudfile.cmcc.com/types"> <filename>日程表.xls</filename>
     * <filesize>3625</filesize> <downloadurl>http://abc.com</downloadurl> </cloudfile>
     */

    private StringBuilder mAccumulator;
    private CloudFileInfoDocument mCouldFile;
    private static Logger sLogger = Logger.getLogger(CloudFileInfoParser.class.getName());
    private final InputSource mInputSource;

    /**
     * Constructor
     *
     * @param inputSource Input source
     */
    public CloudFileInfoParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the PIDF input
     * 
     * @return CloudFileInfoParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public CloudFileInfoParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(mInputSource, this);
            return this;

        } catch (IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    public CloudFileInfoDocument getCouldFileInfo() {
        return mCouldFile;
    }

    @Override
    public void startDocument() {
        mAccumulator = new StringBuilder();
    }

    @Override
    public void characters(char buffer[], int start, int length) {
        mAccumulator.append(buffer, start, length);
    }

    @Override
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        mAccumulator.setLength(0);
        if ("cloudfile".equals(localName)) {
            mCouldFile = new CloudFileInfoDocument();
        }
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
        if ("filename".equals(localName)) {
            if (mCouldFile != null) {
                mCouldFile.setFilename(mAccumulator.toString().trim());
            }
        } else if ("filesize".equals(localName)) {
            if (mCouldFile != null) {
                mCouldFile.setFilesize(Long.parseLong(mAccumulator.toString().trim()));
            }
        } else if ("downloadurl".equals(localName)) {
            if (mCouldFile != null) {
                mCouldFile.setDownloadurl(mAccumulator.toString().trim());
            }
        } else if ("cloudfile".equals(localName)) {
            if (sLogger.isActivated()) {
                sLogger.debug("CouldFile-Info document complete");
            }
        }
    }

}
