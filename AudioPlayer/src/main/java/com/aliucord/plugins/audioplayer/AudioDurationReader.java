package com.aliucord.plugins.audioplayer;

import android.media.MediaMetadataRetriever;
import com.aliucord.Logger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

class AudioDurationReader {
    private static final Logger logger = new Logger("AudioDurationReader");

    static int readDurationMs(String url) {
        if (url != null) {
            String lowerUrl = url.toLowerCase();
            if (lowerUrl.contains(".ogg") || lowerUrl.contains(".opus")) {
                int duration = readOggDurationRangeMs(url);
                if (duration > 0) return duration;
            }
        }

        int duration = readRetrieverDurationMs(url);
        if (duration > 0) return duration;

        return readOggDurationMs(url);
    }

    private static int readRetrieverDurationMs(String url) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(url, new HashMap<String, String>());
            String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            return durationStr != null ? Integer.parseInt(durationStr) : 0;
        } catch (Throwable t) {
            logger.error("MediaMetadataRetriever duration failed", t);
            return 0;
        } finally {
            try {
                retriever.release();
            } catch (Throwable ignored) {}
        }
    }

    private static int readOggDurationRangeMs(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("Range", "bytes=0-4095");

            int responseCode = connection.getResponseCode();
            if (responseCode != 206 && responseCode != 200) {
                connection.disconnect();
                return 0;
            }

            InputStream input = connection.getInputStream();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            input.close();
            connection.disconnect();

            byte[] headerData = output.toByteArray();
            if (headerData.length == 0) return 0;

            int sampleRate = parseSampleRate(headerData);
            int preSkip = parsePreSkip(headerData);
            if (sampleRate <= 0) return 0;

            if (responseCode == 200) {
                // Server doesn't support Range, returned the whole file
                long lastGranule = parseLastGranule(headerData);
                if (lastGranule <= 0) return 0;
                long samples = Math.max(0L, lastGranule - preSkip);
                long duration = (samples * 1000L) / sampleRate;
                return duration > 0L && duration <= Integer.MAX_VALUE ? (int) duration : 0;
            } else {
                // Server supports Range, fetch the last 8KB
                byte[] footerData = fetchRange(url, "bytes=-8192");
                if (footerData == null || footerData.length == 0) return 0;

                long lastGranule = parseLastGranule(footerData);
                if (lastGranule <= 0) return 0;

                long samples = Math.max(0L, lastGranule - preSkip);
                long duration = (samples * 1000L) / sampleRate;
                return duration > 0L && duration <= Integer.MAX_VALUE ? (int) duration : 0;
            }
        } catch (Throwable t) {
            logger.error("Failed to parse OGG duration via range request", t);
            return 0;
        }
    }

    private static byte[] fetchRange(String urlStr, String rangeHeader) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlStr).openConnection();
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        connection.setRequestProperty("Range", rangeHeader);

        int responseCode = connection.getResponseCode();
        if (responseCode != 206 && responseCode != 200) {
            connection.disconnect();
            return null;
        }

        InputStream input = connection.getInputStream();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        input.close();
        connection.disconnect();
        return output.toByteArray();
    }

    private static int readOggDurationMs(String url) {
        if (url == null) return 0;
        String lowerUrl = url.toLowerCase();
        if (!lowerUrl.contains(".ogg") && !lowerUrl.contains(".opus")) return 0;

        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(15000);

            input = connection.getInputStream();
            byte[] data = readLimited(input, 64 * 1024 * 1024);
            return parseOggDurationMs(data);
        } catch (Throwable t) {
            logger.error("Failed to parse OGG duration", t);
            return 0;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable ignored) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static byte[] readLimited(InputStream input, int limit) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int read;

        while ((read = input.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new Exception("Audio file too large to parse duration");
            }
            output.write(buffer, 0, read);
        }

        return output.toByteArray();
    }

    private static int parseOggDurationMs(byte[] data) {
        if (data == null || data.length < 32) return 0;

        int offset = 0;
        int sampleRate = 0;
        int preSkip = 0;
        long lastGranule = -1L;

        while (offset + 27 <= data.length) {
            if (data[offset] != 'O' || data[offset + 1] != 'g' || data[offset + 2] != 'g' || data[offset + 3] != 'S') {
                offset++;
                continue;
            }

            int pageSegments = data[offset + 26] & 0xff;
            int segmentTableOffset = offset + 27;
            int bodyOffset = segmentTableOffset + pageSegments;
            if (bodyOffset > data.length) break;

            int bodySize = 0;
            for (int i = 0; i < pageSegments; i++) {
                bodySize += data[segmentTableOffset + i] & 0xff;
            }

            if (bodyOffset + bodySize > data.length) break;

            long granule = readLittleEndianLong(data, offset + 6);
            if (granule >= 0) {
                lastGranule = granule;
            }

            if (sampleRate == 0 && bodySize >= 19 && startsWith(data, bodyOffset, "OpusHead")) {
                sampleRate = 48000;
                preSkip = readLittleEndianUnsignedShort(data, bodyOffset + 10);
            } else if (sampleRate == 0 && bodySize >= 30 && (data[bodyOffset] & 0xff) == 1 && startsWith(data, bodyOffset + 1, "vorbis")) {
                sampleRate = readLittleEndianInt(data, bodyOffset + 12);
            }

            offset = bodyOffset + bodySize;
        }

        if (sampleRate <= 0 || lastGranule <= 0) return 0;

        long samples = Math.max(0L, lastGranule - preSkip);
        long duration = (samples * 1000L) / sampleRate;
        return duration > 0L && duration <= Integer.MAX_VALUE ? (int) duration : 0;
    }

    private static int parseSampleRate(byte[] data) {
        int offset = 0;
        while (offset + 27 <= data.length) {
            if (data[offset] != 'O' || data[offset + 1] != 'g' || data[offset + 2] != 'g' || data[offset + 3] != 'S') {
                offset++;
                continue;
            }
            int pageSegments = data[offset + 26] & 0xff;
            int bodyOffset = offset + 27 + pageSegments;
            int bodySize = 0;
            for (int i = 0; i < pageSegments; i++) {
                bodySize += data[offset + 27 + i] & 0xff;
            }
            if (bodyOffset + bodySize > data.length) break;

            if (bodySize >= 19 && startsWith(data, bodyOffset, "OpusHead")) {
                return 48000;
            } else if (bodySize >= 30 && (data[bodyOffset] & 0xff) == 1 && startsWith(data, bodyOffset + 1, "vorbis")) {
                return readLittleEndianInt(data, bodyOffset + 12);
            }
            offset = bodyOffset + bodySize;
        }
        return 0;
    }

    private static int parsePreSkip(byte[] data) {
        int offset = 0;
        while (offset + 27 <= data.length) {
            if (data[offset] != 'O' || data[offset + 1] != 'g' || data[offset + 2] != 'g' || data[offset + 3] != 'S') {
                offset++;
                continue;
            }
            int pageSegments = data[offset + 26] & 0xff;
            int bodyOffset = offset + 27 + pageSegments;
            int bodySize = 0;
            for (int i = 0; i < pageSegments; i++) {
                bodySize += data[offset + 27 + i] & 0xff;
            }
            if (bodyOffset + bodySize > data.length) break;

            if (bodySize >= 19 && startsWith(data, bodyOffset, "OpusHead")) {
                return readLittleEndianUnsignedShort(data, bodyOffset + 10);
            }
            offset = bodyOffset + bodySize;
        }
        return 0;
    }

    private static long parseLastGranule(byte[] data) {
        int offset = 0;
        long lastGranule = -1L;
        while (offset + 27 <= data.length) {
            if (data[offset] != 'O' || data[offset + 1] != 'g' || data[offset + 2] != 'g' || data[offset + 3] != 'S') {
                offset++;
                continue;
            }
            int pageSegments = data[offset + 26] & 0xff;
            int bodyOffset = offset + 27 + pageSegments;
            int bodySize = 0;
            for (int i = 0; i < pageSegments; i++) {
                bodySize += data[offset + 27 + i] & 0xff;
            }
            if (bodyOffset + bodySize > data.length) {
                if (offset + 14 <= data.length) {
                    long granule = readLittleEndianLong(data, offset + 6);
                    if (granule >= 0) {
                        lastGranule = granule;
                    }
                }
                break;
            }

            long granule = readLittleEndianLong(data, offset + 6);
            if (granule >= 0) {
                lastGranule = granule;
            }
            offset = bodyOffset + bodySize;
        }
        return lastGranule;
    }

    private static boolean startsWith(byte[] data, int offset, String value) {
        if (offset < 0 || offset + value.length() > data.length) return false;
        for (int i = 0; i < value.length(); i++) {
            if (data[offset + i] != (byte) value.charAt(i)) return false;
        }
        return true;
    }

    private static int readLittleEndianUnsignedShort(byte[] data, int offset) {
        if (offset + 2 > data.length) return 0;
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static int readLittleEndianInt(byte[] data, int offset) {
        if (offset + 4 > data.length) return 0;
        return (data[offset] & 0xff) |
                ((data[offset + 1] & 0xff) << 8) |
                ((data[offset + 2] & 0xff) << 16) |
                ((data[offset + 3] & 0xff) << 24);
    }

    private static long readLittleEndianLong(byte[] data, int offset) {
        if (offset + 8 > data.length) return -1L;
        return ((long) data[offset] & 0xffL) |
                (((long) data[offset + 1] & 0xffL) << 8) |
                (((long) data[offset + 2] & 0xffL) << 16) |
                (((long) data[offset + 3] & 0xffL) << 24) |
                (((long) data[offset + 4] & 0xffL) << 32) |
                (((long) data[offset + 5] & 0xffL) << 40) |
                (((long) data[offset + 6] & 0xffL) << 48) |
                (((long) data[offset + 7] & 0xffL) << 56);
    }
}
