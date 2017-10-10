package tn.ln.httpproxyinjection.net;

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.platform.Platform;
import okio.Buffer;
import okio.BufferedSource;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static okhttp3.internal.http.StatusLine.HTTP_CONTINUE;

/**
 * Created by heshiqi on 17/9/25.
 */

public class HttpLogInterceptor implements Interceptor {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    /**
     * 不打印log日志
     */
    public static final int LEVEL_NONE = 0x01;

    /**
     * 打印请求与请求行
     * <p>示例
     * <pre>{@code
     * --> POST /greeting http/1.1 (3-byte body)
     *
     * <-- 200 OK (22ms,6-byte body)
     *
     * }
     */
    public static final int LEVEL_BASIC = 0x011;

    /**
     * 打印请求、请求行与请求头
     * <p>
     * <p>示例:
     * <pre>{@code
     * --> POST /greeting http/1.1
     * Host: example.com
     * Content-Type: plain/text
     * Content-Length: 3
     * --> END POST
     *
     * <-- 200 OK (22ms)
     * Content-Type: plain/text
     * Content-Length: 6
     * <-- END HTTP
     * }</pre>
     */
    public static final int LEVEL_HEADERS = 0x0111;

    /**
     * 打印请求、请求行、请求头与实体数据
     * <p>
     * <p>示例:
     * <pre>{@code
     * --> POST /greeting http/1.1
     * Host: example.com
     * Content-Type: plain/text
     * Content-Length: 3
     *
     * Hi?
     * --> END GET
     *
     * <-- 200 OK (22ms)
     * Content-Type: plain/text
     * Content-Length: 6
     *
     * Hello!
     * <-- END HTTP
     * }</pre>
     */
    public static final int LEVEL_BODY = 0x1111;


    public interface Logger {

        void log(String message);

        /**
         * A {@link Logger} 默认的日志输出类
         */
        Logger DEFAULT = new Logger() {
            @Override
            public void log(String message) {
                System.out.println(message);
            }
        };
    }

    private volatile int level = LEVEL_NONE;

    /**
     * 日志打印类
     */
    private final Logger logger;

    public HttpLogInterceptor() {
        this(Logger.DEFAULT);
    }

    public HttpLogInterceptor(Logger logger) {
        this.logger = logger;
    }

    /**
     * 设置打印log日志的级别
     *
     * @see #LEVEL_NONE
     * @see #LEVEL_BASIC
     * @see #LEVEL_HEADERS
     * @see #LEVEL_BODY
     */
    public HttpLogInterceptor setLevel(int level) {
        switch (level) {
            case LEVEL_BASIC:
            case LEVEL_HEADERS:
            case LEVEL_BODY:
                this.level = level;
                break;
            default:
                this.level = LEVEL_NONE;
                break;
        }
        return this;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request request = chain.request();
        if (level == LEVEL_NONE) {
            return chain.proceed(request);
        }

        boolean logBody = level == LEVEL_BODY;
        boolean logHeaders = logBody || level == LEVEL_HEADERS;

        /**************************请求部分打印开始********************************/
        logger.log("-->request info start......");
        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;

        String requestStartMessage = request.method() + ' ' + request.url() + ' ' + protocol;
        if (!logHeaders && hasRequestBody) {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
        }
        logger.log(requestStartMessage);

        if (logHeaders) {

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++) {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name)) {
                    logger.log(name + ": " + headers.value(i));
                }
            }

            if (!logBody || !hasRequestBody) {
                logger.log(request.method());
            } else if (bodyEncoded(request.headers())) {
                logger.log(request.method() + " (encoded body omitted)");
            } else {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset(UTF8);
                }
                logger.log("\n");
                if (isPlaintext(buffer)) {
                    logger.log(buffer.readString(charset));
                    logger.log( request.method()
                            + " (" + requestBody.contentLength() + "-byte body)");
                } else {
                    logger.log(request.method() + " (binary "
                            + requestBody.contentLength() + "-byte body omitted)");
                }
            }

        }

        logger.log("-->request info end");
        /**************************请求部分打印完毕********************************/

        logger.log(" ");
        logger.log(" ");
        logger.log(" ");
        logger.log(" ");
        logger.log(" ");

        /**************************响应部分打印开始********************************/
        logger.log("<--response info start......");
        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            logger.log("HTTP FAILED: " + e);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
        logger.log(response.code() + ' ' + response.message() + ' '
                + response.request().url() + " (" + tookMs + "ms" + (!logHeaders ? ", "
                + bodySize + " body" : "") + ')');
        if (logHeaders) {

            if (!logBody || !hasBody(response)) {
                logger.log("END HTTP");
            } else if (bodyEncoded(response.headers())) {
                logger.log("END HTTP (encoded body omitted)");
            } else {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE);
                Buffer buffer = source.buffer();
                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null) {
                    try {
                        charset = contentType.charset(UTF8);
                    } catch (UnsupportedCharsetException e) {
                        logger.log("\n");
                        logger.log("Couldn't decode the response body; charset is likely malformed.");
                        logger.log("END HTTP");
                        return response;
                    }
                }

                if (!isPlaintext(buffer)) {
                    logger.log("\n");
                    logger.log("END HTTP (binary " + buffer.size() + "-byte body omitted)");
                    return response;
                }
                if (contentLength != 0) {
                    logger.log("\n");
                    printResponseBodyString(buffer.clone().readString(charset));
                }

                logger.log("END HTTP (" + buffer.size() + "-byte body)");
            }
        }
        logger.log("-->response info end");
        /**************************响应部分打印完毕********************************/
        return response;
    }

    private void printResponseBodyString(String responseStr) {
        int strLength = responseStr.length();
        int start = 0;
        int end = 2000;
        for (int i = 0; i < 100; i++) {
            if (strLength > end) {
                logger.log(responseStr.substring(start, end));
                start = end;
                end = end + 2000;
            } else {
                logger.log(responseStr.substring(start, strLength));
                break;
            }
        }
    }


    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    static boolean isPlaintext(Buffer buffer) throws EOFException {
        try {
            Buffer prefix = new Buffer();
            long byteCount = buffer.size() < 64 ? buffer.size() : 64;
            buffer.copyTo(prefix, 0, byteCount);
            for (int i = 0; i < 16; i++) {
                if (prefix.exhausted()) {
                    break;
                }
                int codePoint = prefix.readUtf8CodePoint();
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false;
                }
            }
            return true;
        } catch (EOFException e) {
            return false; // Truncated UTF-8 sequence.
        }
    }

    /**
     * Returns true if the response must have a (possibly 0-length) body. See RFC 2616 section 4.3.
     */
    public static boolean hasBody(Response response) {
        // HEAD requests never yield a body regardless of the response headers.
        if (response.request().method().equals("HEAD")) {
            return false;
        }

        int responseCode = response.code();
        if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
                && responseCode != HTTP_NO_CONTENT
                && responseCode != HTTP_NOT_MODIFIED) {
            return true;
        }

        // If the Content-Length or Transfer-Encoding headers disagree with the
        // response code, the response is malformed. For best compatibility, we
        // honor the headers.
        if (contentLength(response) != -1
                || "chunked".equalsIgnoreCase(response.header("Transfer-Encoding"))) {
            return true;
        }

        return false;
    }

    public static long contentLength(Response response) {
        return contentLength(response.headers());
    }

    public static long contentLength(Headers headers) {
        return stringToLong(headers.get("Content-Length"));
    }

    private static long stringToLong(String s) {
        if (s == null) return -1;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private boolean bodyEncoded(Headers headers) {
        //服务器返回的对应的类型编码
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }
}
