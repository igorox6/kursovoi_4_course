package org.example.kursovoi_4_course_1.DBClasses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class UserSettings {
    private Long userId;
    private Integer modelBboxId;
    private Integer modelKeypointsId;
    private TypeDisplay typeDisplay;
    private OffsetDateTime updatedAt;

    private transient Gson gson = new Gson();

    public static List<UserSettings> parseUserSettingsArray(String jsonArray) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> maps = gson.fromJson(jsonArray, listType);

        List<UserSettings> settings = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            settings.add(fromMap(m));
        }
        return settings;
    }

    private static UserSettings fromMap(Map<String, Object> m) {
        UserSettings us = new UserSettings();

        if (m.get("userId") != null) us.userId = ((Number) m.get("userId")).longValue();

        Object bboxObj = m.get("modelBBox");
        if (bboxObj instanceof Map<?, ?> bboxMap) {
            if (bboxMap.get("id") != null) {
                us.modelBboxId = ((Number) bboxMap.get("id")).intValue();
            }
        }

        Object keypointsObj = m.get("modelKeypoints");
        if (keypointsObj instanceof Map<?, ?> keypointsMap) {
            if (((Map<?, ?>) keypointsObj).get("id") != null) {
                us.modelKeypointsId = ((Number) keypointsMap.get("id")).intValue();
            }
        }

        Object displayObj = m.get("typeDisplay");
        if (displayObj != null) {
            us.typeDisplay = TypeDisplay.valueOf(displayObj.toString().toUpperCase());
        }

        if (m.get("updatedAt") != null) us.updatedAt = OffsetDateTime.parse((String) m.get("updatedAt"));

        return us;
    }
}
