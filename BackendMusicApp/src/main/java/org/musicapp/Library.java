package org.musicapp;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Library {

    @Id
    private String id;
    private String userId;
    private User user;
    private List<String> songIds;
    private List<Song> songlists;

}
