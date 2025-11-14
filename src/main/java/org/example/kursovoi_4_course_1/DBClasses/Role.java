package org.example.kursovoi_4_course_1.DBClasses;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
public class Role {
    private Integer id;
    private String name;
    private String description;

    private transient Gson gson = new Gson();

    public RoleType getType() {
        return RoleType.valueOf(this.name.toUpperCase());
    }

    public static List<Role> parseRolesArray(String jsonArray) {
        Gson gson = new Gson();
        Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> maps = gson.fromJson(jsonArray, listType);

        List<Role> roles = new ArrayList<>();
        for (Map<String, Object> m : maps) {
            roles.add(fromMap(m));
        }
        return roles;
    }

    private static Role fromMap(Map<String, Object> m) {
        Role r = new Role();

        if (m.get("id") != null) r.id = ((Number) m.get("id")).intValue();
        r.name = (String) m.get("name");
        r.description = (String) m.get("description");

        return r;
    }
}
