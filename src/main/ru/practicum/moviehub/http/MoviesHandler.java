package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {
    private MoviesStore moviesStore;
    int idMovieList = 0;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] pathArray = path.split("/");
        String queryYear = exchange.getRequestURI().getQuery();

        switch (method) {
            case "GET" -> {
                if (queryYear != null) {
                    int qYear = Integer.parseInt(queryYear.split("=")[1]);
                    List<Movie> findeMovie = moviesStore.getFromStore().entrySet().stream()
                            .map(Map.Entry::getValue)
                            .filter(movie -> movie.getYear() == qYear)
                            .collect(Collectors.toList());
                    sendJson(exchange, 200, gson.toJson(findeMovie));
                }
                if (pathArray.length > 2) {
                    try {
                        int id = Integer.parseInt(pathArray[2]);
                        if (!moviesStore.getFromStore().containsKey(id)) {
                            sendJson(exchange, 404, "Фильм не найден");
                        } else {
                            sendJson(exchange, 200, gson.toJson(moviesStore.getFromStore().get(id)));
                        }
                    } catch (NumberFormatException e) {
                        sendJson(exchange, 400, "Не верный формат ввода");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    sendJson(exchange, 200, gson.toJson(moviesStore.getFromStore().values()));
                }
            }
            case "POST" -> {
                String contentType = exchange.getRequestHeaders().getFirst("Content-type");
                if (contentType != null && !contentType.toLowerCase().startsWith(CT_JSON.toLowerCase())) {
                    sendJson(exchange, 415, "Ошибка заголовка или ключа");
                    return;
                }
                postMovie(exchange);
            }
            case "DELETE" -> {
                if (pathArray.length > 2) {
                    if (pathArray[2].equalsIgnoreCase("all")) {
                        moviesStore.clearMap();
                        idMovieList = 0;
                        sendNoContent(exchange);
                    }
                    try {
                        int id = Integer.parseInt(pathArray[2]);
                        if (!moviesStore.getFromStore().containsKey(id)) {
                            sendJson(exchange, 404, "Фильм не найден");
                        } else {
                            moviesStore.getFromStore().remove(id);
                            sendNoContent(exchange);
                        }
                    } catch (NumberFormatException e) {
                        sendJson(exchange, 400, gson.toJson(new ErrorResponse("Ошибка ввода", List.of("Введите ЧИСЛО."))));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            default -> {
                sendJson(exchange, 405, "Method Not Allowed");
            }
        }
    }

    private void postMovie(HttpExchange exchange) throws IOException {
        Optional movieOptional = getJson(exchange);
        if (movieOptional.isPresent()) {
            Movie movie = (Movie) movieOptional.get();
            int curentYear = LocalDate.now().getYear() + 1;
            int year = movie.getYear();

            List<String> errors = new ArrayList<>();
            if (movie.getTitle().isBlank()) errors.add("название не должно быть пустым");
            if (movie.getTitle().length() > 100) errors.add("слишком длинное название");
            if (year < 1888 || year > curentYear) errors.add("год должен быть между 1888 и 2026");
            if (!errors.isEmpty()) {
                ErrorResponse message = new ErrorResponse("Ошибка валидации", errors);
                sendJson(exchange, 422, errorToJson(message));
                return;
            }
            idMovieList++;
            moviesStore.addToStore(idMovieList, movie);
            sendJson(exchange, 201, idMovieList + ": "
                    + gson.toJson(moviesStore.getFromStore().get(idMovieList)));
        }
    }


}