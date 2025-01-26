package org.musicapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.logging.Logger;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MyRestService {
    private final MongoTemplate mongoTemplate;
    private final UserRepository usersRepository;
    private Logger logger = Logger.getLogger("My Rest Service");

    private RestClient restClient;

    @Autowired
    public MyRestService(MongoTemplate mongoTemplate, UserRepository usersRepository) {
        this.mongoTemplate = mongoTemplate;
        this.usersRepository = usersRepository;
    }

    public List<User> getAllUsers() {
        logger.info("getAllUsers");
        List<User> users = mongoTemplate.findAll(User.class, "users");
        return users;
    }

    public List<Artist> getAllArtists() {
        logger.info("getAllArtists");
        List<Artist> artists = mongoTemplate.findAll(Artist.class, "artists");
        return artists;
    }

    public List<Song> getAllSongs() {
        logger.info("getAllSongs");
        List<Song> songs = mongoTemplate.findAll(Song.class, "songs");

        // Ręczne pobieranie obiektu Artist na podstawie artistId
        for (Song song : songs) {
            if (song.getArtistId() != null) {
                Artist artist = mongoTemplate.findById(song.getArtistId(), Artist.class, "artists");
                song.setArtist(artist); // Ustaw pełny obiekt Artist w polu artist
            }
        }
        return songs;
    }

    public List<Album> getAllAlbums() {
        logger.info("getAllAlbums");
        List<Album> albums = mongoTemplate.findAll(Album.class, "albums");

        for (Album album : albums) {
            // Pobieranie artysty na podstawie artistId
            if (album.getArtistId() != null) {
                Artist artist = mongoTemplate.findById(album.getArtistId(), Artist.class, "artists");
                album.setArtist(artist);
            }

            // Pobieranie piosenek na podstawie songIds
            if (album.getSongIds() != null && !album.getSongIds().isEmpty()) {
                List<Song> songs = mongoTemplate.find(Query.query(Criteria.where("id").in(album.getSongIds())), Song.class, "songs");

                // Ustawianie artysty w każdej piosence
                for (Song song : songs) {
                    if (song.getArtistId() != null) {
                        Artist songArtist = mongoTemplate.findById(song.getArtistId(), Artist.class, "artists");
                        song.setArtist(songArtist);
                    }
                }
                album.setSongs(songs);
            }
        }

        return albums;
    }

    public List<Playlist> getAllPlaylists() {
        logger.info("getAllPlaylists");
        List<Playlist> playlists = mongoTemplate.findAll(Playlist.class, "playlists");

        for (Playlist playlist : playlists) {
            // Pobieranie usera na podstawie userId
            if (playlist.getUserId() != null) {
                User user = mongoTemplate.findById(playlist.getUserId(), User.class, "users");
                playlist.setUser(user);
            }

            // Pobieranie piosenek na podstawie songIds
            if (playlist.getSongIds() != null && !playlist.getSongIds().isEmpty()) {
                List<Song> songs = mongoTemplate.find(Query.query(Criteria.where("id").in(playlist.getSongIds())), Song.class, "songs");

                // Ustawianie artysty w każdej piosence
                for (Song song : songs) {
                    if (song.getArtistId() != null) {
                        Artist songArtist = mongoTemplate.findById(song.getArtistId(), Artist.class, "artists");
                        song.setArtist(songArtist);
                    }
                }
                playlist.setSongs(songs);
            }
        }

        return playlists;
    }

    public Library getLibraryByUserId(String userId) {
        logger.info("Fetching library for userId: " + userId);
        Query query = new Query(Criteria.where("userId").is(userId));
        Library library = mongoTemplate.findOne(query, Library.class, "libraries");

        if (library != null && library.getUserId() != null) {
            User user = mongoTemplate.findById(library.getUserId(), User.class, "users");
            library.setUser(user);

            if (library.getSongIds() != null && !library.getSongIds().isEmpty()) {
                List<Song> songs = mongoTemplate.find(Query.query(Criteria.where("id").in(library.getSongIds())), Song.class, "songs");

                // Fetch artist data for each song
                for (Song song : songs) {
                    if (song.getArtistId() != null) {
                        Artist artist = mongoTemplate.findById(song.getArtistId(), Artist.class, "artists");
                        song.setArtist(artist);
                    }
                }

                library.setSonglists(songs);
            }
        }

        return library;
    }



    public List<Artist> searchArtists(String name, String country, List<String> genre) {
        logger.info("Searching artists with name: " + name + ", country: " + country + ", genres: " + genre);

        Query query = new Query();

        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("name").regex(".*" + name + ".*", "i")); // Case-insensitive
        }
        if (country != null && !country.isEmpty()) {
            query.addCriteria(Criteria.where("country").regex("^" + country + "$", "i"));
        }
        if (genre != null && !genre.isEmpty()) {
            query.addCriteria(Criteria.where("genre").in(
                    genre.stream()
                            .map(g -> java.util.regex.Pattern.compile("^" + g + "$", java.util.regex.Pattern.CASE_INSENSITIVE))
                            .toList()
            ));
        }

        logger.info("Mongo query: " + query.toString());

        List<Artist> results = mongoTemplate.find(query, Artist.class, "artists");

        logger.info("Found " + results.size() + " artists");

        return results;
    }


    public List<Song> searchSongs(String title, String artistName, Integer year, String artistId) {
        logger.info("searchSongs");
        Query query = new Query();


        if (artistId != null && !artistId.isEmpty()) {
            query.addCriteria(Criteria.where("artistId").is(artistId));
        }
        if (title != null && !title.isEmpty()) {
            query.addCriteria(Criteria.where("title").regex(".*" + title + ".*", "i")); // Case-insensitive
        }
        if (artistName != null && !artistName.isEmpty()) {
            // Pobieranie artystów pasujących do nazwy
            List<Artist> artists = mongoTemplate.find(
                    Query.query(Criteria.where("name").regex(".*" + artistName + ".*", "i")),
                    Artist.class,
                    "artists"
            );
            List<String> artistIds = artists.stream().map(Artist::getId).toList();
            query.addCriteria(Criteria.where("artistId").in(artistIds));
        }
        if (year != null) {
            query.addCriteria(Criteria.where("year").is(year));
        }

        List<Song> songs = mongoTemplate.find(query, Song.class, "songs");

        // Ustawianie pełnych obiektów Artist w wynikach
        for (Song song : songs) {
            if (song.getArtistId() != null) {
                Artist artist = mongoTemplate.findById(song.getArtistId(), Artist.class, "artists");
                song.setArtist(artist);
            }
        }

        return songs;
    }

    public Artist addArtist(Artist artist) {
        logger.info("Saving new artist: " + artist.getName());
        return mongoTemplate.save(artist, "artists");
    }

    public User addUser(User user) {
        logger.info("Attempting to add user: " + user.getUserName());

        // Check if a user with the same email already exists
        User existingUser = mongoTemplate.findOne(Query.query(Criteria.where("email").is(user.getEmail())), User.class);

        if (existingUser != null) {
            // If user exists, throw an exception or return an error message
            logger.warning("User with email " + user.getEmail() + " already exists.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User with this email already exists.");
        }

        // Save the new user
        User newUser = mongoTemplate.save(user, "users");

        // Create a library for the new user containing an empty list of songs
        Library library = new Library();
        library.setUserId(newUser.getId());
        library.setSongIds(new ArrayList<>()); // Empty list of songs
        mongoTemplate.save(library, "libraries");

        return newUser;
    }


    public Artist updateArtist(String id, Artist updatedArtist) {
        logger.info("Updating artist with ID: " + id);

        // Sprawdzenie, czy artysta istnieje
        Artist existingArtist = mongoTemplate.findById(id, Artist.class, "artists");
        if (existingArtist == null) {
            throw new IllegalArgumentException("Artist with ID " + id + " does not exist.");
        }

        // Aktualizacja pól
        existingArtist.setName(updatedArtist.getName());
        existingArtist.setBirthday(updatedArtist.getBirthday());
        existingArtist.setDeathday(updatedArtist.getDeathday());
        existingArtist.setCountry(updatedArtist.getCountry());
        existingArtist.setGenre(updatedArtist.getGenre());
        existingArtist.setActivityPeriod(updatedArtist.getActivityPeriod());

        // Zapis do bazy danych
        mongoTemplate.save(existingArtist, "artists");

        return existingArtist;
    }

    public User updateUser(String id, User updatedUser) {
        logger.info("Updating user with ID: " + id);
        User existingUser = mongoTemplate.findById(id, User.class, "users");
        if (existingUser == null) {
            throw new IllegalArgumentException("User with ID " + id + " does not exist.");
        }

        existingUser.setUserName(updatedUser.getUserName());
        existingUser.setAge(updatedUser.getAge());

        mongoTemplate.save(existingUser, "users");

        return existingUser;
    }

    public boolean deleteArtist(String id) {
        logger.info("Deleting artist with ID: " + id);

        // Sprawdzenie, czy artysta istnieje
        Artist existingArtist = mongoTemplate.findById(id, Artist.class, "artists");
        if (existingArtist == null) {
            throw new IllegalArgumentException("Artist with ID " + id + " does not exist.");
        }

        // Usuwanie artysty
        mongoTemplate.remove(existingArtist, "artists");
        return true;
    }

    public boolean deleteUser(String id) {
        logger.info("Deleting user with ID: " + id);

        // Sprawdzenie, czy artysta istnieje
        User existingUser = mongoTemplate.findById(id, User.class, "users");
        if (existingUser == null) {
            throw new IllegalArgumentException("User with ID " + id + " does not exist.");
        }

        // Usuwanie artysty
        mongoTemplate.remove(existingUser, "users");
        return true;
    }

    public List<Album> searchAlbums(String title) {
        logger.info("searchAlbums");
        Query query = new Query();

        if (title != null && !title.isEmpty()) {
            query.addCriteria(Criteria.where("title").regex(".*" + title + ".*", "i")); // Case-insensitive
        }

        List<Album> albums = mongoTemplate.find(query, Album.class, "albums");

        // Ustawianie pełnych obiektów Artist w wynikach
        for (Album album : albums) {
            if (album.getArtistId() != null) {
                Artist artist = mongoTemplate.findById(album.getArtistId(), Artist.class, "artists");
                album.setArtist(artist);
            }
        }

        return albums;
    }

    public Artist getArtistById(String artistId) {
        logger.info("Fetching artist with ID: " + artistId);

        // Pobieranie artysty z bazy danych na podstawie ID
        Artist artist = mongoTemplate.findById(artistId, Artist.class, "artists");

        // Sprawdzenie, czy artysta istnieje
        if (artist == null) {
            throw new IllegalArgumentException("Artist with ID " + artistId + " does not exist.");
        }

        return artist;
    }

    public List<Playlist> searchPlaylists(String title) {
        // Pobierz wszystkie playlisty
        List<Playlist> allPlaylists = getAllPlaylists();
        // Filtruj playlisty na podstawie tytułu
        return allPlaylists.stream() .filter(playlist -> title == null || playlist.getTitle().toLowerCase().contains(title.toLowerCase())) .collect(Collectors.toList());
    }

    public User getUserById(String userId) {
        logger.info("Fetching user with ID: " + userId);
        User user = mongoTemplate.findById(userId, User.class, "users");

        if (user == null) {
            throw new IllegalArgumentException("User with ID " + userId + " does not exist.");
        }

        return user;
    }

    public String authenticateUser(String email, String password) {
        // Find the user by email
        User user = mongoTemplate.findOne(Query.query(Criteria.where("email").is(email)), User.class);

        if (user == null) {
            // If no user is found with the given email, return an error message
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        // Check if the password matches
        if (!user.getPassword().equals(password)) {
            // If the password does not match, return an error message
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        // If email and password are correct, return the user's ID
        return user.getId();
    }

    public Library modifyLibrary(String userId, String songId, boolean addSong) {
        logger.info("Modifying library for userId: " + userId + ", songId: " + songId + ", addSong: " + addSong);
        Query query = new Query(Criteria.where("userId").is(userId));
        Library library = mongoTemplate.findOne(query, Library.class, "libraries");

        if (library != null) {
            List<String> songIds = library.getSongIds();

            if (addSong) {
                if (!songIds.contains(songId)) {
                    songIds.add(songId);
                }
            } else {
                songIds.remove(songId);
            }

            library.setSongIds(songIds);
            mongoTemplate.save(library, "libraries");

            // Fetch user details
            User user = mongoTemplate.findById(library.getUserId(), User.class, "users");
            library.setUser(user);

            // Fetch song details including artist data
            if (library.getSongIds() != null && !library.getSongIds().isEmpty()) {
                List<Song> songs = mongoTemplate.find(Query.query(Criteria.where("id").in(library.getSongIds())), Song.class, "songs");

                for (Song song : songs) {
                    if (song.getArtistId() != null) {
                        Artist artist = mongoTemplate.findById(song.getArtistId(), Artist.class, "artists");
                        song.setArtist(artist);
                    }
                }

                library.setSonglists(songs);
            }
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Library not found for userId: " + userId);
        }

        return library;
    }

    public String getSongLink(String songId) {
        logger.info("Fetching link to file for songId: " + songId);
        Song song = mongoTemplate.findById(songId, Song.class, "songs");

        if (song != null) {
            return song.getLinkToFile();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Song not found for songId: " + songId);
        }
    }


    public String getAlbumLink(String albumId) {
        logger.info("Fetching link to file for albumId: " + albumId);
        Album album = mongoTemplate.findById(albumId, Album.class, "albums");

        if (album != null) {
            String linkToFile = album.getLinkToFile();

            if (linkToFile == null || linkToFile.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Album found but link to file is empty or not set");
            }

            return linkToFile;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found for albumId: " + albumId);
        }
    }

    public Album getAlbumBySongId(String songId) {
        logger.info("Fetching album for songId in rest service: " + songId);

        try {
            // Znajdź album zawierający piosenkę o podanym songId
            Query query = Query.query(Criteria.where("songIds").in(songId));
            Album album = mongoTemplate.findOne(query, Album.class, "albums");

            if (album == null) {
                // Rzuć wyjątek 404, jeśli album nie istnieje
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found for songId: " + songId);
            }

            return album;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error occurred");
        }
    }

    public Playlist createPlaylistForUser(Playlist playlist) {
        // Fetch the User object by userId
        User user = mongoTemplate.findById(playlist.getUserId(), User.class);
        if (user == null) {
            return null; // Return null if the user is not found
        }

        // If there are song IDs, fetch the Song objects; otherwise, initialize an empty list
        List<Song> songs = new ArrayList<>();
        if (playlist.getSongIds() != null && !playlist.getSongIds().isEmpty()) {
            songs = getSongsByIds(playlist.getSongIds());
        }

        // Set the User object and Songs into the Playlist
        playlist.setUser(user);
        playlist.setSongs(songs);

        // Save the Playlist into MongoDB
        return savePlaylist(playlist);
    }

    // Helper method to fetch songs by their IDs
    public List<Song> getSongsByIds(List<String> songIds) {
        Query query = new Query(Criteria.where("id").in(songIds));
        return mongoTemplate.find(query, Song.class);
    }

    // Method to save the Playlist into MongoDB
    public Playlist savePlaylist(Playlist playlist) {
        try {
            mongoTemplate.save(playlist, "playlists");
            return playlist; // Return the created playlist
        } catch (Exception e) {
            // Log the error and return null if saving fails
            return null;
        }
    }

    public boolean deletePlaylistById(String playlistId) {
        // Sprawdzamy, czy istnieje playlista o podanym ID
        Playlist playlist = mongoTemplate.findById(playlistId, Playlist.class, "playlists");
        if (playlist == null) {
            throw new IllegalArgumentException("User with ID " + playlistId + " does not exist.");
        }

        try {
            mongoTemplate.remove(playlist, "playlists"); // Usuwamy playlistę
            return true; // Udane usunięcie
        } catch (Exception e) {
            // Logujemy błąd w przypadku niepowodzenia
            logger.info("Error while deleting playlist with id: " + playlistId);
            return false;
        }
    }


}
