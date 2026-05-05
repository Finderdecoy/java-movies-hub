package ru.practicum.moviehub.model;

import java.time.LocalDate;

public class Movie {
    private String title;
    private LocalDate year;
    private int id;

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public int getYear() {
        return year.getYear();
    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", year=" + year +
                '}';
    }
}