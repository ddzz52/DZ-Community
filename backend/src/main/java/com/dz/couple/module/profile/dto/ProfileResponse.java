package com.dz.couple.module.profile.dto;

import com.dz.couple.module.user.dto.UserVO;

public class ProfileResponse {
    private String signature;
    private int loveDays;
    private String loveAnniversaryText;
    private Stats stats;
    private UserVO me;
    private UserVO partner;
    private Boolean partnerOnline;

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public int getLoveDays() {
        return loveDays;
    }

    public void setLoveDays(int loveDays) {
        this.loveDays = loveDays;
    }

    public String getLoveAnniversaryText() {
        return loveAnniversaryText;
    }

    public void setLoveAnniversaryText(String loveAnniversaryText) {
        this.loveAnniversaryText = loveAnniversaryText;
    }

    public Stats getStats() {
        return stats;
    }

    public void setStats(Stats stats) {
        this.stats = stats;
    }

    public UserVO getMe() {
        return me;
    }

    public void setMe(UserVO me) {
        this.me = me;
    }

    public UserVO getPartner() {
        return partner;
    }

    public void setPartner(UserVO partner) {
        this.partner = partner;
    }

    public Boolean getPartnerOnline() {
        return partnerOnline;
    }

    public void setPartnerOnline(Boolean partnerOnline) {
        this.partnerOnline = partnerOnline;
    }

    public static class Stats {
        private long anniversaries;
        private long diaries;
        private long photos;

        public long getAnniversaries() {
            return anniversaries;
        }

        public void setAnniversaries(long anniversaries) {
            this.anniversaries = anniversaries;
        }

        public long getDiaries() {
            return diaries;
        }

        public void setDiaries(long diaries) {
            this.diaries = diaries;
        }

        public long getPhotos() {
            return photos;
        }

        public void setPhotos(long photos) {
            this.photos = photos;
        }
    }
}

