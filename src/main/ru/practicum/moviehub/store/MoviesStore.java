package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.LinkedList;
import java.util.List;

public class MoviesStore {
    private List<Movie> movies;

    public MoviesStore() {
        movies = new LinkedList<>();
    }

    public void addToStore(Movie movie) {
        movies.add(movie);
    }

    public List<Movie> getFromStore() {
        return movies;
    }

    @Override
    public String toString() {
        return movies.toString();
    }
}