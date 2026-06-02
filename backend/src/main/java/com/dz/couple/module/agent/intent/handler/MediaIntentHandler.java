package com.dz.couple.module.agent.intent.handler;

import com.dz.couple.module.agent.dto.AgentChatResponse;
import com.dz.couple.module.agent.intent.IntentEnum;
import com.dz.couple.module.album.dto.AlbumVO;
import com.dz.couple.module.album.service.AlbumService;
import com.dz.couple.module.photo.dto.PhotoVO;
import com.dz.couple.module.photo.service.PhotoService;
import com.dz.couple.module.sticker.service.StickerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MediaIntentHandler implements IntentHandler {

    private static final Set<IntentEnum> SUPPORTED = EnumSet.of(
            IntentEnum.QUERY_PHOTO,
            IntentEnum.QUERY_ALBUM,
            IntentEnum.QUERY_STICKER
    );

    @Autowired
    private PhotoService photoService;

    @Autowired
    private AlbumService albumService;

    @Autowired
    private StickerService stickerService;

    @Override
    public Set<IntentEnum> supportedIntents() {
        return SUPPORTED;
    }

    @Override
    public AgentChatResponse handle(Long userId, Long coupleId, String message, Map<String, Object> params) {
        List<String> actions = new ArrayList<>();
        String reply;

        if (message.contains("贴纸") || message.contains("贴图") || message.contains("sticker")) {
            reply = "贴纸功能在聊天页面可以使用哦~ 去和TA聊天时发送可爱贴纸吧！🎨";
            actions.add("查看贴纸");

        } else if (message.contains("相册") || message.contains("相簿")) {
            List<AlbumVO> albums = albumService.list(coupleId);
            if (albums.isEmpty()) {
                reply = "还没有相册呢~ 去上传你们的照片吧！📸";
            } else {
                StringBuilder sb = new StringBuilder("📸 你们的相册：\n\n");
                for (AlbumVO a : albums) {
                    if (a == null) continue;
                    String name = a.getName() != null ? a.getName() : "未命名";
                    long count = a.getPhotoCount() != null ? a.getPhotoCount() : 0;
                    sb.append("  ").append(name).append(" (").append(count).append("张)\n");
                }
                sb.append("\n去看看相册里的美好回忆吧~ 💕");
                reply = sb.toString();
            }
            actions.add("查询相册");

        } else {
            // 照片
            List<PhotoVO> photos = photoService.list(userId, coupleId, null, null, null, false, false, 5);
            if (photos.isEmpty()) {
                reply = "还没有上传照片呢~ 去相册上传你们的美好瞬间吧！📸";
            } else {
                StringBuilder sb = new StringBuilder("🖼️ 最近的照片：\n\n");
                for (int i = 0; i < Math.min(3, photos.size()); i++) {
                    PhotoVO p = photos.get(i);
                    String title = p.getTitle() != null ? p.getTitle() : "未命名";
                    String mood = p.getMood() != null ? "[" + p.getMood() + "] " : "";
                    sb.append("  ").append(title).append(" ").append(mood).append("\n");
                }
                sb.append("\n去相册查看更多照片吧~ 📸");
                reply = sb.toString();
            }
            actions.add("查询照片");
        }

        AgentChatResponse resp = new AgentChatResponse();
        resp.setReply(reply);
        resp.setEmotion("开心");
        resp.setExecutedActions(actions);
        resp.setNeedFollowUp(false);
        return resp;
    }
}
