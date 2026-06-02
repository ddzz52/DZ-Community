package com.dz.couple.module.message.ws;

import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.security.JwtUtil;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Map;

@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public JwtHandshakeInterceptor(JwtUtil jwtUtil, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        String deviceId = extractParam(request, "deviceId");
        Long userId;
        try {
            userId = jwtUtil.parseUserId(token);
        } catch (Exception e) {
            return false;
        }
        if (userId == null) {
            return false;
        }
        User user = userMapper.findById(userId);
        if (user == null || user.getCoupleId() == null) {
            return false;
        }
        attributes.put("userId", userId);
        attributes.put("coupleId", user.getCoupleId());
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            attributes.put("deviceId", deviceId.trim());
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(ServerHttpRequest req) {
        String q = extractParam(req, "token");
        if (q != null && !q.trim().isEmpty()) {
            return q.trim();
        }
        if (req instanceof ServletServerHttpRequest) {
            HttpServletRequest r = ((ServletServerHttpRequest) req).getServletRequest();
            String auth = r.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                return auth.substring("Bearer ".length()).trim();
            }
            if (auth != null && !auth.trim().isEmpty()) {
                return auth.trim();
            }
        }
        return null;
    }

    private String extractParam(ServerHttpRequest req, String key) {
        if (key == null || key.trim().isEmpty()) {
            return null;
        }
        if (req instanceof ServletServerHttpRequest) {
            HttpServletRequest r = ((ServletServerHttpRequest) req).getServletRequest();
            String q = r.getParameter(key);
            if (q != null && !q.trim().isEmpty()) {
                return q.trim();
            }
        }
        URI uri = req.getURI();
        if (uri != null && uri.getQuery() != null) {
            for (String part : uri.getQuery().split("&")) {
                int idx = part.indexOf('=');
                if (idx > 0) {
                    String k = part.substring(0, idx);
                    String v = part.substring(idx + 1);
                    if (key.equals(k)) {
                        return v;
                    }
                }
            }
        }
        return null;
    }
}
