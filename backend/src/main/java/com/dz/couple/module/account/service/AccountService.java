package com.dz.couple.module.account.service;

import com.dz.couple.common.BusinessException;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.account.dto.AccountCategoryVO;
import com.dz.couple.module.account.dto.AccountCreateRequest;
import com.dz.couple.module.account.dto.AccountMonthStatsResponse;
import com.dz.couple.module.account.dto.AccountUpdateRequest;
import com.dz.couple.module.account.dto.AccountVO;
import com.dz.couple.module.account.dto.AccountYearStatsResponse;
import com.dz.couple.module.account.entity.Account;
import com.dz.couple.module.account.mapper.AccountMapper;
import com.dz.couple.module.couple.entity.Couple;
import com.dz.couple.module.couple.mapper.CoupleMapper;
import com.dz.couple.module.user.entity.User;
import com.dz.couple.module.user.mapper.UserMapper;
import com.dz.couple.module.message.ws.ChatHub;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {
    private final AccountMapper accountMapper;
    private final UserMapper userMapper;
    private final ChatHub chatHub;
    private final CoupleMapper coupleMapper;

    @Autowired
    public AccountService(AccountMapper accountMapper, UserMapper userMapper, ChatHub chatHub, CoupleMapper coupleMapper) {
        this.accountMapper = accountMapper;
        this.userMapper = userMapper;
        this.chatHub = chatHub;
        this.coupleMapper = coupleMapper;
    }

    public List<AccountVO> list(Long userId,
                                Long coupleId,
                                String category,
                                Date from,
                                Date to,
                                Date beforeAt,
                                Long beforeId,
                                int limit) {
        ensureMember(userId, coupleId);
        int l = Math.max(1, Math.min(limit, 200));
        List<Account> list = accountMapper.list(coupleId, category, from, to, beforeAt, beforeId, l);
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        List<AccountVO> out = new ArrayList<>();
        for (Account a : list) {
            out.add(toVO(a));
        }
        return out;
    }

    public AccountVO create(Long userId, Long coupleId, AccountCreateRequest req) {
        ensureMember(userId, coupleId);
        if (req == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        String cat = safeTrim(req.getCategory());
        if (cat == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择分类");
        }
        BigDecimal amt = req.getAmount();
        if (amt == null || amt.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额需≥0.01");
        }
        Date occurredAt = parseDateTime(req.getOccurredAt());
        Date now = new Date();
        Account a = new Account();
        a.setCoupleId(coupleId);
        a.setUserId(userId);
        a.setCategory(cat);
        a.setAmount(amt);
        a.setOccurredAt(occurredAt == null ? now : occurredAt);
        a.setRemark(safeTrim(req.getRemark()));
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        accountMapper.insert(a);
        pushChanged(coupleId, userId, "CREATED", a.getId());
        return toVO(a);
    }

    public AccountVO update(Long userId, Long coupleId, Long id, AccountUpdateRequest req) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Account cur = accountMapper.findById(coupleId, id);
        if (cur == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        String cat = safeTrim(req == null ? null : req.getCategory());
        if (cat == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请选择分类");
        }
        BigDecimal amt = req.getAmount();
        if (amt == null || amt.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "金额需≥0.01");
        }
        Date occurredAt = parseDateTime(req.getOccurredAt());
        cur.setCategory(cat);
        cur.setAmount(amt);
        if (occurredAt != null) {
            cur.setOccurredAt(occurredAt);
        }
        cur.setRemark(safeTrim(req.getRemark()));
        cur.setUpdatedAt(new Date());
        int n = accountMapper.update(cur);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        pushChanged(coupleId, userId, "UPDATED", cur.getId());
        return toVO(cur);
    }

    public void delete(Long userId, Long coupleId, Long id) {
        ensureMember(userId, coupleId);
        if (id == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        int n = accountMapper.delete(coupleId, id);
        if (n <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        pushChanged(coupleId, userId, "DELETED", id);
    }

    public AccountMonthStatsResponse monthStats(Long userId, Long coupleId, int year, int month) {
        ensureMember(userId, coupleId);
        if (year < 1970 || year > 2100 || month < 1 || month > 12) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Date from = startOfMonth(year, month);
        Date to = startOfMonth(year, month + 1);

        AccountMonthStatsResponse r = new AccountMonthStatsResponse();
        r.setYear(year);
        r.setMonth(month);
        r.setFrom(from);
        r.setTo(to);
        BigDecimal total = accountMapper.sumAmount(coupleId, from, to);
        if (total == null) total = BigDecimal.ZERO;
        r.setTotalAmount(total);
        r.setByCategory(accountMapper.sumByCategory(coupleId, from, to));
        r.setByUser(accountMapper.sumByUser(coupleId, from, to));

        Couple couple = coupleMapper.findById(coupleId);
        if (couple != null && couple.getMonthlyBudget() != null && couple.getMonthlyBudget().compareTo(BigDecimal.ZERO) > 0) {
            r.setMonthlyBudget(couple.getMonthlyBudget());
            r.setOverBudget(total.compareTo(couple.getMonthlyBudget()) > 0);
            r.setBudgetRemaining(couple.getMonthlyBudget().subtract(total));
        }
        return r;
    }

    public AccountYearStatsResponse yearStats(Long userId, Long coupleId, int year) {
        ensureMember(userId, coupleId);
        if (year < 1970 || year > 2100) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Date from = startOfYear(year);
        Date to = startOfYear(year + 1);

        AccountYearStatsResponse r = new AccountYearStatsResponse();
        r.setYear(year);
        r.setFrom(from);
        r.setTo(to);
        BigDecimal total = accountMapper.sumAmount(coupleId, from, to);
        r.setTotalAmount(total == null ? BigDecimal.ZERO : total);
        r.setByMonth(accountMapper.sumByMonth(coupleId, from, to));
        r.setByCategory(accountMapper.sumByCategory(coupleId, from, to));
        return r;
    }

    public List<AccountCategoryVO> listCategories(Long userId, Long coupleId, int limit) {
        ensureMember(userId, coupleId);
        int l = Math.max(1, Math.min(limit, 100));
        List<AccountCategoryVO> list = accountMapper.listCategories(coupleId, l);
        return list == null ? Collections.<AccountCategoryVO>emptyList() : list;
    }

    private AccountVO toVO(Account a) {
        AccountVO vo = new AccountVO();
        vo.setId(a.getId());
        vo.setUserId(a.getUserId());
        vo.setCategory(a.getCategory());
        vo.setAmount(a.getAmount());
        vo.setOccurredAt(a.getOccurredAt());
        vo.setRemark(a.getRemark());
        vo.setCreatedAt(a.getCreatedAt());
        vo.setUpdatedAt(a.getUpdatedAt());
        return vo;
    }

    private void ensureMember(Long userId, Long coupleId) {
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        User u = userMapper.findById(userId);
        if (u == null || u.getCoupleId() == null || !u.getCoupleId().equals(coupleId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void pushChanged(Long coupleId, Long actorUserId, String action, Long accountId) {
        if (coupleId == null || actorUserId == null || accountId == null) {
            return;
        }
        List<User> users = userMapper.listByCoupleId(coupleId);
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("action", action);
        data.put("id", accountId);
        data.put("at", System.currentTimeMillis());

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "ACCOUNT_CHANGED");
        payload.put("data", data);

        for (User u : users) {
            if (u == null || u.getId() == null) continue;
            if (u.getId().equals(actorUserId)) continue;
            chatHub.push(u.getId(), payload);
        }
    }

    private String safeTrim(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private Date parseDateTime(String s) {
        String t = safeTrim(s);
        if (t == null) return null;
        t = t.replace('T', ' ');
        if (t.endsWith("Z")) {
            t = t.substring(0, t.length() - 1);
        }
        int dot = t.indexOf('.');
        if (dot > 0) {
            t = t.substring(0, dot);
        }
        try {
            if (t.length() == 10) {
                SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
                return f.parse(t);
            }
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return f.parse(t);
        } catch (ParseException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时间格式错误");
        }
    }

    private Date startOfYear(int year) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, Calendar.JANUARY);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date startOfMonth(int year, int month) {
        int y = year;
        int m = month;
        if (m <= 0) {
            int delta = (Math.abs(m) / 12) + 1;
            y -= delta;
            m += 12 * delta;
        }
        if (m > 12) {
            int delta = (m - 1) / 12;
            y += delta;
            m = m - 12 * delta;
        }
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, y);
        c.set(Calendar.MONTH, m - 1);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
