package io.cloudbeaver.server.jetty;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.jkiss.utils.IOUtils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {
    private final String body;

    public static void main(String[] args) {

    }
    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        // 读取并缓存请求体
        body = IOUtils.readToString(request.getReader());
    }

    public String getBody() {
        return body;
    }

    @Override
    public ServletInputStream getInputStream() {
        // 返回基于缓存体的输入流
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        return new ServletInputStream() {
            @Override
            public boolean isFinished() { return byteArrayInputStream.available() == 0; }

            @Override
            public boolean isReady() { return true; }

            @Override
            public void setReadListener(ReadListener listener) { }

            @Override
            public int read() { return byteArrayInputStream.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        // 返回基于缓存体的Reader
        return new BufferedReader(new StringReader(body));
    }
}
