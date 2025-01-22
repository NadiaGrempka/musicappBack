package org.musicapp;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserDtoMapper {
    public static UserDto userToDto(User user){
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setName(user.getUserName());
        //dto.setEmail(user.getEmail());
        return dto;
    }
}
