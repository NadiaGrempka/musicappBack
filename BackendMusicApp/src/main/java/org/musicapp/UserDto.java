package org.musicapp;

import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
public class UserDto {
    private String  userId;
    private String  userName;
    @Setter
    private String  email;


    public String getId() {
        return userId;
    }

    public void setId(String id) {
        this.userId = userId;
    }

    public String getName() {
        return userName;
    }

    public void setName(String name) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

}
