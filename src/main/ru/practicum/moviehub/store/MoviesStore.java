package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.Collection;
import java.util.HashMap;

public class MoviesStore {
    private HashMap<Integer, Movie> movies;
    private int id = 0;

    public MoviesStore() {
        movies = new HashMap();
    }

    public Movie addToStore(Movie movie) {
        id++;
        movies.put(id, movie);
        return movie;
    }

    public Collection<Movie> getFromStore() throws NullPointerException {
        return movies.values();
    }

    public void clearMap() {
        id = 0;
        movies.clear();
    }

    public Movie getMovie(int id) {
        return movies.get(id);
    }

    public Movie delete(int id) {
        return movies.remove(id);
    }

}