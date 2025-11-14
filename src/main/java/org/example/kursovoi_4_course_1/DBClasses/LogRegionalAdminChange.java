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
public class LogRegionalAdminChange {
    private Integer id;
    private Long userId;
    private Integer changeFromModelId;
    private Integer changeToModelId;
    private OffsetDateTime timeChange;

    private transient Gson gson = new Gson();

    public static List<LogRegionalAdminChange> parseLogRegionalAdminChangesArray(String jsonArray) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> maps = gson.fromJson(jsonArray, listType);

        List<LogRegionalAdminChange> changes = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            changes.add(fromMap(m));
        }
        return changes;
    }

    private static LogRegionalAdminChange fromMap(Map<String, Object> m) {
        LogRegionalAdminChange lc = new LogRegionalAdminChange();

        if (m.get("id") != null) lc.id = ((Number) m.get("id")).intValue();

        Object userObj = m.get("user");
        if (userObj instanceof Map<?, ?> userMap) {
            if (userMap.get("id") != null) {
                lc.userId = ((Number) userMap.get("id")).longValue();
            }
        }

        Object fromModelObj = m.get("changeFromModel");
        if (fromModelObj instanceof Map<?, ?> fromMap) {
            if (fromMap.get("id") != null) {
                lc.changeFromModelId = ((Number) fromMap.get("id")).intValue();
            }
        }

        Object toModelObj = m.get("changeToModel");
        if (toModelObj instanceof Map<?, ?> toMap) {
            if (toMap.get("id") != null) {
                lc.changeToModelId = ((Number) toMap.get("id")).intValue();
            }
        }

        if (m.get("timeChange") != null) lc.timeChange = OffsetDateTime.parse((String) m.get("timeChange"));

        return lc;
    }
}
