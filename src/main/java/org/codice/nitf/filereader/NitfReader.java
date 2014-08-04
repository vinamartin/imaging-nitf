/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.nitf.filereader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class NitfReader {
    private BufferedInputStream input = null;
    private int numBytesRead = 0;
    private FileType nitfFileType = FileType.UNKNOWN;

    private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    private static final int STANDARD_DATE_TIME_LENGTH = 14;
    private static final int ENCRYP_LENGTH = 1;
    private static final String DATE_ONLY_DAY_FORMAT = "yyyyMMdd";
    private static final String DATE_FULL_FORMAT = "yyyyMMddHHmmss";
    private static final String NITF20_DATE_FORMAT = "ddHHmmss'Z'MMMyy";
    private static final int RGB_COLOUR_LENGTH = 3;
    private static final String GENERIC_READ_ERROR_MESSAGE = "Error reading from NITF stream: ";

    public NitfReader(final BufferedInputStream nitfInputStream, final int offset) throws ParseException {
        input = nitfInputStream;
        numBytesRead = offset;
    }

    public final void setFileType(final FileType fileType) {
        nitfFileType = fileType;
    }

    public final FileType getFileType() {
        return nitfFileType;
    }

    public final Boolean canSeek() {
        return false;
    }

    public final int getNumBytesRead() {
        return numBytesRead;
    }

    public final void verifyHeaderMagic(final String magicHeader) throws ParseException {
        String actualHeader = readBytes(magicHeader.length());
        if (!actualHeader.equals(magicHeader)) {
            throw new ParseException(String.format("Missing \'%s\' magic header, got \'%s\'", magicHeader, actualHeader), numBytesRead);
        }
    }

    public final RGBColour readRGBColour() throws ParseException {
        byte[] rgb = readBytesRaw(RGB_COLOUR_LENGTH);
        return new RGBColour(rgb);
    }

    public final Date readNitfDateTime() throws ParseException {
        String dateString = readTrimmedBytes(STANDARD_DATE_TIME_LENGTH);
        SimpleDateFormat dateFormat = null;
        Date dateTime = null;
        switch (nitfFileType) {
            case NITF_TWO_ZERO:
                dateFormat = new SimpleDateFormat(NITF20_DATE_FORMAT);
                break;
            case NITF_TWO_ONE:
            case NSIF_ONE_ZERO:
                dateFormat = new SimpleDateFormat(DATE_FULL_FORMAT);
                break;
            case UNKNOWN:
            default:
                throw new ParseException("Need to set NITF file type prior to reading dates", numBytesRead);
        }
        if (dateString.length() == DATE_ONLY_DAY_FORMAT.length()) {
            // Fallback for files that aren't spec compliant
            dateFormat = new SimpleDateFormat(DATE_ONLY_DAY_FORMAT);
        } else if (dateString.length() == 0) {
            // This can't work
            dateFormat = null;
        } else if (dateString.length() != DATE_FULL_FORMAT.length()) {
            System.out.println("Unhandled date format:" + dateString);
        }
        if (dateFormat == null) {
            dateTime = null;
        } else {
            dateTime = dateFormat.parse(dateString);
            if (dateTime == null) {
                throw new ParseException(String.format("Bad DATETIME format: %s", dateString), numBytesRead);
            }
        }
        return dateTime;
    }

    public final Integer readBytesAsInteger(final int count) throws ParseException {
        String intString = readBytes(count);
        // System.out.println("Bytes to be converted to integer: |" + intString + "|");
        Integer intValue = 0;
        try {
            intValue = Integer.parseInt(intString);
        } catch (NumberFormatException ex) {
            throw new ParseException(String.format("Bad Integer format: [%s]", intString), numBytesRead);
        }
        return intValue;
    }

    public final Long readBytesAsLong(final int count) throws ParseException {
        String longString = readBytes(count);
        Long longValue = 0L;
        try {
            longValue = Long.parseLong(longString);
        } catch (NumberFormatException ex) {
            throw new ParseException(String.format("Bad Long format: %s", longString), numBytesRead);
        }
        return longValue;
    }

    public final Double readBytesAsDouble(final int count) throws ParseException {
        String doubleString = readBytes(count);
        Double doubleValue = 0.0;
        try {
            doubleValue = Double.parseDouble(doubleString.trim());
        } catch (NumberFormatException ex) {
            throw new ParseException(String.format("Bad Double format: %s", doubleString), numBytesRead);
        }
        return doubleValue;
    }

    public final String readTrimmedBytes(final int count) throws ParseException {
        return rightTrim(readBytes(count));
    }

    public static String rightTrim(final String s) {
        int i = s.length() - 1;
        while ((i >= 0) && Character.isWhitespace(s.charAt(i))) {
            i--;
        }
        return s.substring(0, i + 1);
    }

    public final void readENCRYP() throws ParseException {
        if (!"0".equals(readBytes(ENCRYP_LENGTH))) {
            throw new ParseException("Unexpected ENCRYP value", numBytesRead);
        }
    }

    public final String readBytes(final int count) throws ParseException {
        return new String(readBytesRaw(count), UTF8_CHARSET);
    }

    public final byte[] readBytesRaw(final int count) throws ParseException {
        try {
            byte[] bytes = new byte[count];
            int thisRead = input.read(bytes, 0, count);
            if (thisRead == -1) {
                throw new ParseException("End of file reading from NITF stream.", numBytesRead);
            } else if (thisRead < count) {
                throw new ParseException(String.format("Short read while reading from NITF stream (%s/%s).", thisRead, count),
                                         numBytesRead + thisRead);
            }
            numBytesRead += thisRead;
            return bytes;
        } catch (IOException ex) {
            throw new ParseException(GENERIC_READ_ERROR_MESSAGE + ex.getMessage(), numBytesRead);
        }
    }

    public final void skip(final long count) throws ParseException {
        long bytesToRead = count;
        try {
            long thisRead = 0;
            do {
                thisRead = input.skip(bytesToRead);
                numBytesRead += thisRead;
                bytesToRead -= thisRead;
            } while (bytesToRead > 0);
        } catch (IOException ex) {
            throw new ParseException(GENERIC_READ_ERROR_MESSAGE + ex.getMessage(), numBytesRead);
        }
    }
}
