package com.dz.couple.module.memo.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MemoEditLockManager {
    public static class LockInfo {
        private Long userId;
        private String nickname;
        private long expireAtMs;

        public LockInfo(Long userId, String nickname, long expireAtMs) {
            this.userId = userId;
            this.nickname = nickname;
            this.expireAtMs = expireAtMs;
        }

        public Long getUserId() {
            return userId;
        }

        public String getNickname() {
            return nickname;
        }

        public long getExpireAtMs() {
            return expireAtMs;
        }

        public void setExpireAtMs(long expireAtMs) {
            this.expireAtMs = expireAtMs;
        }
    }

    public static class TryLockResult {
        private boolean ok;
        private LockInfo lockedBy;

        public TryLockResult(boolean ok, LockInfo lockedBy) {
            this.ok = ok;
            this.lockedBy = lockedBy;
        }

        public boolean isOk() {
            return ok;
        }

        public LockInfo getLockedBy() {
            return lockedBy;
        }
    }

    private final Map<String, LockInfo> locks = new ConcurrentHashMap<>();

    public TryLockResult tryLock(Long coupleId, Long memoId, Long userId, String nickname, long ttlMs) {
        if (coupleId == null || memoId == null || userId == null) {
            return new TryLockResult(false, null);
        }
        String key = key(coupleId, memoId);
        long now = System.currentTimeMillis();
        long exp = now + Math.max(1000, ttlMs);
        while (true) {
            LockInfo cur = locks.get(key);
            if (cur == null || cur.getExpireAtMs() <= now) {
                LockInfo next = new LockInfo(userId, nickname, exp);
                LockInfo prev = locks.put(key, next);
                if (prev == null || prev.getExpireAtMs() <= now || (prev.getUserId() != null && prev.getUserId().equals(userId))) {
                    return new TryLockResult(true, next);
                } else {
                    locks.put(key, prev);
                    return new TryLockResult(false, prev);
                }
            }
            if (cur.getUserId() != null && cur.getUserId().equals(userId)) {
                cur.setExpireAtMs(exp);
                return new TryLockResult(true, cur);
            }
            return new TryLockResult(false, cur);
        }
    }

    public boolean heartbeat(Long coupleId, Long memoId, Long userId, long ttlMs) {
        if (coupleId == null || memoId == null || userId == null) {
            return false;
        }
        String key = key(coupleId, memoId);
        LockInfo cur = locks.get(key);
        if (cur == null || cur.getUserId() == null || !cur.getUserId().equals(userId)) {
            return false;
        }
        long exp = System.currentTimeMillis() + Math.max(1000, ttlMs);
        cur.setExpireAtMs(exp);
        return true;
    }

    public void release(Long coupleId, Long memoId, Long userId) {
        if (coupleId == null || memoId == null || userId == null) {
            return;
        }
        String key = key(coupleId, memoId);
        LockInfo cur = locks.get(key);
        if (cur != null && cur.getUserId() != null && cur.getUserId().equals(userId)) {
            locks.remove(key);
        }
    }

    public LockInfo getValidLock(Long coupleId, Long memoId) {
        if (coupleId == null || memoId == null) {
            return null;
        }
        String key = key(coupleId, memoId);
        LockInfo cur = locks.get(key);
        if (cur == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (cur.getExpireAtMs() <= now) {
            locks.remove(key);
            return null;
        }
        return cur;
    }

    private String key(Long coupleId, Long memoId) {
        return coupleId + ":" + memoId;
    }
}
