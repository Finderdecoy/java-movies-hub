package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {
    private MoviesStore moviesStore;
    private Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String[] pathArray = path.split("/");
        gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        String queryYear = exchange.getRequestURI().getQuery();

        switch (method) {
            case "GET" -> {
                if (queryYear != null) {
                    int qYear = Integer.parseInt(queryYear.split("=")[1]);
                    List<Movie> findeMovie = moviesStore.getFromStore().stream()
                            .filter(film -> film.getYear() == qYear)
                            .collect(Collectors.toList());
                    sendJson(exchange, 200, gson.toJson(findeMovie));
                }
                if (pathArray.length > 2) {
                    try {
                        int id = Integer.parseInt(pathArray[2]) - 1;
                        if (id < 0 || id >= moviesStore.getFromStore().size()) {
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
                    sendJson(exchange, 200, gson.toJson(moviesStore.getFromStore()));
                }
            }
            case "POST" -> {
                String contenType = exchange.getRequestHeaders().getFirst("Content-type");
                if (!contenType.equals("application/json; charset=UTF-8")) {
                    sendJson(exchange, 415, "Ошибка заголовка или ключа");
                    return;
                }
                postMovie(exchange);
            }
            case "DELETE" -> {
                if (pathArray.length > 2) {
                    if (pathArray[2].equalsIgnoreCase("all")) {
                        moviesStore.getFromStore().clear();
                        sendNoContent(exchange);
                    }
                    try {
                        int id = Integer.parseInt(pathArray[2]);
                        if (moviesStore.getFromStore().size() < id) {
                            sendJson(exchange, 404, "Фильм не найден");
                        } else {
                            moviesStore.getFromStore().remove(id - 1);
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
            if (movie.getTitle().isBlank() || movie.getTitle().length() > 100 || year < 1888 || year > curentYear) {
                List<String> errors = new ArrayList<>();
                errors.add("название не должно быть пустым");
                errors.add("год должен быть между 1888 и 2026");
                ErrorResponse message = new ErrorResponse("Ошибка валидации", errors);
                sendJson(exchange, 422, errorToJson(message));
                return;
            }
            moviesStore.addToStore(movie);
            sendJson(exchange, 201, "");
        }
    }


}