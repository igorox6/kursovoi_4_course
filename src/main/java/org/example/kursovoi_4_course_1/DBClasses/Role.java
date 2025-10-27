package org.example.kursovoi_4_course_1.DBClasses;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Role {
    private  Integer id;
    private String name;
    private String description;

    public RoleType getType(){
        return RoleType.valueOf(this.name.toUpperCase());
    }
}
