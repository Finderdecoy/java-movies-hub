package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;

public class MoviesStore {
    private HashMap<Integer, Movie> movies;

    public MoviesStore() {
        movies = new HashMap();
    }

    public void addToStore(int id, Movie movie) {
        movies.put(id, movie);
    }

    public HashMap<Integer, Movie> getFromStore() {
        return movies;
    }

    public void clearMap() {
        movies.clear();
    }

    @Override
    public String toString() {
        return movies.toString();
    }
}