/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.hept59434091.reactor.rpc4j.compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.github.hept59434091.reactor.rpc4j.exc.RpcCompressException;

/**
 * @author <a href="luxueneng@baidu.com">luxueneng</a>
 * @since 2019-04-17
 */
public class GZipCompress implements Compress {

    /**
     * default buffer size.
     */
    private static final int BUFFER_SIZE = 256;

    @Override
    public byte[] compress(byte[] array) {
        return compress0(array);
    }

    private byte[] compress0(byte[] array) {
        if (array == null) {
            return null;
        }

        byte[] ret = null;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(array);
            gzip.close();
            ret = out.toByteArray();
        } catch (IOException e) {
            throw new RpcCompressException(e);
        }
        return ret;
    }

    @Override
    public byte[] unCompress(byte[] array) {
        return unCompress0(array);
    }

    private byte[] unCompress0(byte[] array) {
        if (array == null) {
            return null;
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(array);

        try {
            GZIPInputStream gunzip = new GZIPInputStream(in) {

            };
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = gunzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
        } catch (IOException e) {
            throw new RpcCompressException(e);
        }
        return out.toByteArray();
    }

}

