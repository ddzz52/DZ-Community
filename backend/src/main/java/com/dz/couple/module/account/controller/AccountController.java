package com.dz.couple.module.account.controller;

import com.dz.couple.common.ApiResponse;
import com.dz.couple.common.BusinessException;
import com.dz.couple.common.CurrentUser;
import com.dz.couple.common.ErrorCode;
import com.dz.couple.module.account.dto.AccountCategoryVO;
import com.dz.couple.module.account.dto.AccountCreateRequest;
import com.dz.couple.module.account.dto.AccountMonthStatsResponse;
import com.dz.couple.module.account.dto.AccountUpdateRequest;
import com.dz.couple.module.account.dto.AccountVO;
import com.dz.couple.module.account.dto.AccountYearStatsResponse;
import com.dz.couple.module.account.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {
    private final AccountService accountService;

    @Autowired
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ApiResponse<List<AccountVO>> list(@RequestParam(value = "category", required = false) String category,
                                             @RequestParam(value = "from", required = false) String from,
                                             @RequestParam(value = "to", required = false) String to,
                                             @RequestParam(value = "beforeAt", required = false) String beforeAt,
                                             @RequestParam(value = "beforeId", required = false) Long beforeId,
                                             @RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 50 : limit;
        return ApiResponse.ok(accountService.list(userId, coupleId, category, parseDateTime(from), parseToExclusive(to), parseDateTime(beforeAt), beforeId, l));
    }

    @PostMapping
    public ApiResponse<AccountVO> create(@Valid @RequestBody AccountCreateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(accountService.create(userId, coupleId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<AccountVO> update(@PathVariable("id") Long id, @Valid @RequestBody AccountUpdateRequest req) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return ApiResponse.ok(accountService.update(userId, coupleId, id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        accountService.delete(userId, coupleId, id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stats/month")
    public ApiResponse<AccountMonthStatsResponse> monthStats(@RequestParam(value = "year", required = false) Integer year,
                                                            @RequestParam(value = "month", required = false) Integer month) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Calendar c = Calendar.getInstance();
        int y = year == null ? c.get(Calendar.YEAR) : year;
        int m = month == null ? (c.get(Calendar.MONTH) + 1) : month;
        return ApiResponse.ok(accountService.monthStats(userId, coupleId, y, m));
    }

    @GetMapping("/stats/year")
    public ApiResponse<AccountYearStatsResponse> yearStats(@RequestParam(value = "year", required = false) Integer year) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Calendar c = Calendar.getInstance();
        int y = year == null ? c.get(Calendar.YEAR) : year;
        return ApiResponse.ok(accountService.yearStats(userId, coupleId, y));
    }

    @GetMapping("/categories")
    public ApiResponse<List<AccountCategoryVO>> categories(@RequestParam(value = "limit", required = false) Integer limit) {
        Long userId = CurrentUser.getUserId();
        Long coupleId = CurrentUser.getCoupleId();
        if (userId == null || coupleId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        int l = limit == null ? 30 : limit;
        return ApiResponse.ok(accountService.listCategories(userId, coupleId, l));
    }

    private Date parseDateTime(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        String t = s.trim().replace('T', ' ');
        if (t.endsWith("Z")) t = t.substring(0, t.length() - 1);
        int dot = t.indexOf('.');
        if (dot > 0) t = t.substring(0, dot);
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

    private Date parseToExclusive(String s) {
        Date d = parseDateTime(s);
        if (d == null) return null;
        String t = s == null ? "" : s.trim();
        if (t.length() == 10) {
            Calendar c = Calendar.getInstance();
            c.setTime(d);
            c.add(Calendar.DAY_OF_MONTH, 1);
            return c.getTime();
        }
        return d;
    }
}

