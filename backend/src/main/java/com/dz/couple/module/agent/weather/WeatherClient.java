package com.dz.couple.module.agent.weather;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

/**
 * 天气查询客户端 — 调用 wttr.in 免费天气 API
 * 无需 API Key，支持全球城市查询
 */
@Component
public class WeatherClient {

    private static final Logger log = LoggerFactory.getLogger(WeatherClient.class);
    private static final String BASE_URL = "https://wttr.in";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final RestTemplate restTemplate;

    public WeatherClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(8000);
        factory.setReadTimeout(8000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 查询城市天气
     * @param city 城市名（中文或英文）
     * @return 格式化的天气信息 Map，失败返回 null
     */
    public Map<String, Object> query(String city) {
        try {
            String encoded = URLEncoder.encode(city.trim(), "UTF-8");
            String url = BASE_URL + "/" + encoded + "?format=j1&lang=zh";
            log.info("查询天气: city={}, url={}", city, url);

            String resp = restTemplate.getForObject(url, String.class);
            if (resp == null || resp.trim().isEmpty()) return null;

            JsonNode root = JSON.readTree(resp);
            JsonNode current = root.path("current_condition").get(0);
            if (current == null || current.isMissingNode()) return null;

            Map<String, Object> weather = new LinkedHashMap<>();
            weather.put("city", city.trim());
            weather.put("temp", current.path("temp_C").asText());
            weather.put("feelsLike", current.path("FeelsLikeC").asText());
            // 优先取中文描述，fallback 到英文 weatherDesc
            String desc = extractLangZh(current, "未知");
            weather.put("desc", desc);
            weather.put("humidity", current.path("humidity").asText());
            weather.put("windSpeed", current.path("windspeedKmph").asText() + "km/h");
            weather.put("windDir", current.path("winddir16Point").asText());
            weather.put("uvIndex", current.path("uvIndex").asText());
            weather.put("visibility", current.path("visibility").asText() + "km");

            // 提取近3天预报
            JsonNode forecast = root.path("weather");
            if (forecast.isArray() && forecast.size() > 0) {
                Map<String, String> dailyPreviews = new LinkedHashMap<>();
                int days = Math.min(forecast.size(), 3);
                for (int i = 0; i < days; i++) {
                    JsonNode day = forecast.get(i);
                    String date = day.path("date").asText();
                    String max = day.path("maxtempC").asText();
                    String min = day.path("mintempC").asText();
                    String dayDesc = "未知";
                    JsonNode hourly = day.path("hourly");
                    if (hourly.isArray() && hourly.size() > 0) {
                        JsonNode midday = hourly.get(Math.min(hourly.size() / 2, hourly.size() - 1));
                        dayDesc = extractLangZh(midday, "未知");
                    }
                    dailyPreviews.put(date, min + "°C ~ " + max + "°C " + dayDesc);
                }
                weather.put("forecast", dailyPreviews);
            }
            return weather;
        } catch (Exception e) {
            log.warn("天气查询失败: city={}, error={}", city, e.getMessage());
            return null;
        }
    }

    /** 提取中文天气描述：lang_zh → 英文翻译 → 兜底默认值 */
    private String extractLangZh(JsonNode node, String defaultText) {
        // 第一优先级：lang_zh 直接返回中文
        try {
            JsonNode langZhArr = node.path("lang_zh");
            if (langZhArr.isArray() && langZhArr.size() > 0) {
                String val = langZhArr.get(0).path("value").asText(null);
                if (val != null && !val.isEmpty()) return val;
            }
        } catch (Exception ignored) {}
        // 第二优先级：weatherDesc 英文 → 中文映射表翻译
        try {
            JsonNode weatherDescArr = node.path("weatherDesc");
            if (weatherDescArr.isArray() && weatherDescArr.size() > 0) {
                String en = weatherDescArr.get(0).path("value").asText(null);
                if (en != null && !en.isEmpty()) {
                    String zh = EN_TO_CN.get(en.toLowerCase());
                    return zh != null ? zh : en; // 映射表有的翻译，没有的保留原文
                }
            }
        } catch (Exception ignored) {}
        return defaultText;
    }

    /** 常用天气英文→中文映射表 */
    private static final Map<String, String> EN_TO_CN = new HashMap<>();
    static {
        EN_TO_CN.put("sunny", "晴");
        EN_TO_CN.put("clear", "晴");
        EN_TO_CN.put("partly cloudy", "多云");
        EN_TO_CN.put("cloudy", "阴");
        EN_TO_CN.put("overcast", "阴");
        EN_TO_CN.put("mist", "薄雾");
        EN_TO_CN.put("fog", "雾");
        EN_TO_CN.put("freezing fog", "冻雾");
        EN_TO_CN.put("patchy rain possible", "局部可能有雨");
        EN_TO_CN.put("patchy rain nearby", "局部有雨");
        EN_TO_CN.put("light drizzle", "小雨");
        EN_TO_CN.put("drizzle", "毛毛雨");
        EN_TO_CN.put("light rain", "小雨");
        EN_TO_CN.put("light rain shower", "小阵雨");
        EN_TO_CN.put("moderate rain", "中雨");
        EN_TO_CN.put("moderate rain at times", "间歇中雨");
        EN_TO_CN.put("heavy rain", "大雨");
        EN_TO_CN.put("heavy rain at times", "间歇大雨");
        EN_TO_CN.put("torrential rain shower", "暴雨");
        EN_TO_CN.put("rain", "雨");
        EN_TO_CN.put("rain shower", "阵雨");
        EN_TO_CN.put("patchy snow possible", "局部可能有雪");
        EN_TO_CN.put("patchy snow nearby", "局部有雪");
        EN_TO_CN.put("light snow", "小雪");
        EN_TO_CN.put("snow", "雪");
        EN_TO_CN.put("heavy snow", "大雪");
        EN_TO_CN.put("blizzard", "暴风雪");
        EN_TO_CN.put("blowing snow", "吹雪");
        EN_TO_CN.put("sleet", "雨夹雪");
        EN_TO_CN.put("ice pellets", "冰粒");
        EN_TO_CN.put("thunder", "雷");
        EN_TO_CN.put("thunderstorm", "雷暴");
        EN_TO_CN.put("thundery outbreaks possible", "可能有雷暴");
        EN_TO_CN.put("hail", "冰雹");
        EN_TO_CN.put("windy", "大风");
        EN_TO_CN.put("breezy", "微风");
        EN_TO_CN.put("hot", "炎热");
        EN_TO_CN.put("warm", "温暖");
        EN_TO_CN.put("cold", "寒冷");
        EN_TO_CN.put("freezing", "极寒");
        EN_TO_CN.put("humid", "潮湿");
        EN_TO_CN.put("dry", "干燥");
    }
}
