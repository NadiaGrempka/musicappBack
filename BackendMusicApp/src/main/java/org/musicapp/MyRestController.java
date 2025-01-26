package org.musicapp;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CrossOrigin(origins="http://localhost:5173")
@RestController
public class MyRestController {

    private final MyRestService restService;

    @Autowired
    public MyRestController(MyRestService restService) {
        this.restService = restService;
    }

    private Logger logger = Logger.getLogger("My Rest Controller");

    @GetMapping("hello")
    public String hello() {
        return "Hello";
    }

    @GetMapping("/users")
    public List<User> getUsers() {
        logger.info("getUsers");
        return this.restService.getAllUsers();
    }

    @GetMapping("/users/{userId}")
    public User getUserId(@PathVariable String userId) {
        logger.info("Fetching user for userId: " + userId);
        return restService.getUserById(userId);
    }

    @GetMapping("/artists")
    public List<Artist> getArtists() {
        logger.info("getArtists");
        return this.restService.getAllArtists();
    }

    @GetMapping("/artists/{artistId}")
    public Artist getArtistId(@PathVariable String artistId) {
        logger.info("Fetching artist for artistId: " + artistId);
        return restService.getArtistById(artistId);
    }

    @GetMapping("/songs")
    public List<Song> getSongs() {
        logger.info("getSongs");
        return this.restService.getAllSongs();
    }

    @GetMapping("/albums")
    public List<Album> getAlbums() {
        logger.info("getAlbums");
        return this.restService.getAllAlbums();
    }

    @GetMapping("/playlists")
    public List<Playlist> getPlaylists() {
        logger.info("getPlaylists");
        return this.restService.getAllPlaylists();
    }

    @GetMapping("libraries/{userId}")
    public Library getLibrary(@PathVariable String userId) {
        logger.info("Fetching library for userId: " + userId);
        return restService.getLibraryByUserId(userId);
    }

    //todo endpoint library
    @PutMapping("/library")
    public ResponseEntity<Library> modifyLibrary(@RequestParam String userId, @RequestParam String songId, @RequestParam boolean addSong) {
        logger.info("Received request to modify library for userId: " + userId + ", songId: " + songId + ", addSong: " + addSong);
        Library updatedLibrary = restService.modifyLibrary(userId, songId, addSong);
        return ResponseEntity.ok(updatedLibrary);
    }

    @GetMapping("/search/artists")
    public List<Artist> searchArtists(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) List<String> genre) {
        logger.info("Searching artists");
        return restService.searchArtists(name, country, genre);
    }

    @GetMapping("/search/songs")
    public List<Song> searchSongs(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String artistName,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String artistId){
        logger.info("Searching songs");
        return restService.searchSongs(title, artistName, year, artistId);
    }

    @GetMapping("/search/albums")
    public List<Album> searchAlbums(
            @RequestParam(required = false) String title) {
        logger.info("Searching albums");
        return restService.searchAlbums(title);
    }

    @GetMapping("/search/playlists")
    public List<Playlist> searchPlaylists(@RequestParam(required = false) String title) {
        logger.info("Searching playlists");
        return restService.searchPlaylists(title);
    }


    @PostMapping("/artists")
    public Artist addArtist(@RequestBody Artist artist) {
        logger.info("Adding new artist: " + artist.getName());
        return restService.addArtist(artist);
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@RequestBody User user) {
        logger.info("Adding new user: " + user.getUserName());

        try {
            User newUser = restService.addUser(user);
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser); // 201 Created
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason()); // 400 Bad Request if email already exists
        }
    }

    @PutMapping("/artists/{id}")
    public Artist updateArtist(
            @PathVariable String id,
            @RequestBody Artist updatedArtist) {
        logger.info("Updating artist with ID: " + id);
        return restService.updateArtist(id, updatedArtist);
    }

    @PutMapping("/users/{id}")
    public User updateUser(
            @PathVariable String id,
            @RequestBody User updatedUser) {
        logger.info("Updating user with ID: " + id);
        return restService.updateUser(id, updatedUser);
    }

    @DeleteMapping("/artists/{id}")
    public ResponseEntity<String> deleteArtist(@PathVariable String id) {
        logger.info("Deleting artist with ID: " + id);
        boolean isDeleted = restService.deleteArtist(id);

        if (isDeleted) {
            return ResponseEntity.ok("Artist with ID " + id + " has been deleted.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Artist with ID " + id + " not found.");
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        logger.info("Deleting user with ID: " + id);
        boolean isDeleted = restService.deleteUser(id);

        if (isDeleted) {
            return ResponseEntity.ok("User with ID " + id + " has been deleted.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("User with ID " + id + " not found.");
        }
    }

    @PostMapping("/auth")
    public ResponseEntity<String> authenticateUser(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        try {
            // Authenticate the user using the provided email and password
            String userId = restService.authenticateUser(email, password);
            return ResponseEntity.ok(userId); // If successful, return the user's ID
        } catch (ResponseStatusException ex) {
            // If authentication fails, return an error message
            return ResponseEntity.status(ex.getStatusCode()).body(ex.getReason()); // 401 Unauthorized if invalid credentials
        }
    }

    @GetMapping("/songs/{id}/link")
    public ResponseEntity<String> getSongLink(@PathVariable String id, HttpServletRequest request) {
        logger.info("Fetching link to file for songId: " + id);
        String filePath = restService.getSongLink(id);

        // Wyciągnij nazwę pliku z pełnej ścieżki
        String fileName = Paths.get(filePath).getFileName().toString();

        // Generuj URL dostępny dla frontendu
        String fileUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/" + fileName;

        logger.info("Generated file URL: " + fileUrl);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/albums/{id}/link")
    public ResponseEntity<String> getAlbumLink(@PathVariable String id, HttpServletRequest request) {
        logger.info("Fetching link to file for songId: " + id);
        String filePath = restService.getAlbumLink(id);

        // Wyciągnij nazwę pliku z pełnej ścieżki
        String fileName = Paths.get(filePath).getFileName().toString();

        // Generuj URL dostępny dla frontendu
        String fileUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/" + fileName;

        logger.info("Generated file URL: " + fileUrl);
        return ResponseEntity.ok(fileUrl);
    }

    @GetMapping("/albums/info")
    public ResponseEntity<Album> getAlbumBySongId(@RequestParam String songId) {
        logger.info("Fetching album for songId: " + songId);

        // Delegacja całości logiki (wraz z obsługą błędów) do RestService
        Album album = restService.getAlbumBySongId(songId);

        return ResponseEntity.ok(album);
    }

    @PostMapping("/playlists")
    public ResponseEntity<Playlist> createPlaylist(@RequestBody Playlist playlist) {
        logger.info("Creating new playlist for userId: " + playlist.getUserId());

        if (playlist.getSongIds() == null) {
            playlist.setSongIds(new ArrayList<>()); // Ensure songIds is not null, but can be empty
        }

        Playlist newPlaylist = restService.createPlaylistForUser(playlist);

        if (newPlaylist != null) {
            return ResponseEntity.status(HttpStatus.CREATED).body(newPlaylist); // Return the created playlist
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(null); // Return 400 if something went wrong
        }
    }

    @DeleteMapping("/playlists/{id}")
    public ResponseEntity<String> deletePlaylist(@PathVariable String id) {
        logger.info("Deleting playlist with id: " + id);

        boolean deleted = restService.deletePlaylistById(id);

        if (deleted) {
            return ResponseEntity.ok("Playlist with ID " + id + " has been deleted.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Playlist with ID " + id + " not found.");
        }
    }

}
