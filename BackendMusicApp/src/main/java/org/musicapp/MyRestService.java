package org.musicapp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

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
        // Pobieranie usera na podstawie userId
        if (library.getUserId() != null) {
            User user = mongoTemplate.findById(library.getUserId(), User.class, "users");
            library.setUser(user);
        }
        //TODO dodać pobieranie szczegółowych danych o playlistach na podstawie playlistIds
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

        // If no existing user, save the new user
        return mongoTemplate.save(user, "users");
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


}
