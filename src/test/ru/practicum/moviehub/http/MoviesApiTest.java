package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static final String CT = "Content-type";
    private static final String CT_VALUE = "application/json; charset=UTF-8";

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
    }

    @BeforeEach
    void beforeEach() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().DELETE().uri(URI.create(BASE + "/movies/all")).build();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        client.send(req, bodyHandler);
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies")).build();
        HttpResponse.BodyHandler<String> responseBodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> resp = client.send(req, responseBodyHandler);
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        String contentTypeHeaderValue = resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue, "Content-Type должен содержать формат данных и кодировку");
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"), "Ожидается JSON-массив");
    }


    @Test
    void testTitlePost() throws IOException, InterruptedException {
        String titleBlank = "{\"title\": \"\", \"year\": 2010}";
        String tittle101Chars = String.format("{\"title\": %s, \"year\": 2010}", "f".repeat(101));
        String title100Chars = String.format("{\"title\": %s, \"year\": 2010}", "f".repeat(100));

        HttpResponse<String> respTitleBlank = postRequest(titleBlank);
        HttpResponse<String> respTitle100Chars = postRequest(title100Chars);
        HttpResponse<String> respTitle101Chars = postRequest(tittle101Chars);

        assertEquals(422, respTitleBlank.statusCode(), "Не корректное добавление 422");

        assertEquals(422, respTitle101Chars.statusCode(), "Вовзращает ошибку при длине более 100 символов");

        assertEquals(201, respTitle100Chars.statusCode(), "Вернет 201 . Т.к. символов до 100 включительно");

    }

    @Test
    void rightYearPOST() throws IOException, InterruptedException {
        String year2027 = "{\"title\":\"Властелин морей\", \"year\": 2027}";
        String year2028 = "{\"title\":\"Властелин морей\", \"year\": 2028}";
        String year1888 = "{\"title\":\"Властелин морей\", \"year\": 1888}";
        String year1887 = "{\"title\":\"Властелин морей\", \"year\": 1887}";
        String year2010 = "{\"title\":\"Властелин морей\", \"year\": 2010}";

        HttpResponse responseYear1887 = postRequest(year1887);
        HttpResponse responseYear1888 = postRequest(year1888);
        HttpResponse responseYear2027 = postRequest(year2027);
        HttpResponse responseYear2028 = postRequest(year2028);
        HttpResponse responseYear2010 = postRequest(year2010);

        assertEquals(422, responseYear1887.statusCode(), "Год указан НЕ верно должно быть 422");
        assertEquals(201, responseYear1888.statusCode(), "Год указан ВЕРНО должно быть 201");
        assertEquals(201, responseYear2027.statusCode(), "Год указан ВЕРНО должно быть 201");
        assertEquals(422, responseYear2028.statusCode(), "Год указан НЕ верно должно быть 201");
        assertEquals(201, responseYear2010.statusCode(), "Год указан ВЕРНО должно быть 201");
    }

    @Test
    void wrongContentType() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(" ")).uri(URI.create(BASE + "/movies")).header(CT, "nojson").build();
        HttpResponse.BodyHandler<String> responseBodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> response = client.send(request, responseBodyHandler);

        assertEquals(415, response.statusCode(), "Должен вернутся код 415");
    }

    @Test
    void movieGetById() throws IOException, InterruptedException {
        String idString = "asd";
        int idNoCorrect = 0;
        int idCorrect = 4;

        for (int i = 1; i < 5; i++) {
            postRequest(String.format("{\"title\": \"Властелин морей:%s\", \"year\": \"2010\"}", i));
        }

        HttpResponse respString = getByID(idString);
        HttpResponse respNoCorrect = getByID(idNoCorrect);
        HttpResponse respCorrect = getByID(idCorrect);

        assertEquals(200, respCorrect.statusCode(), "Фильм найден");

        assertEquals(404, respNoCorrect.statusCode(), "Фильм не должен быть найден");

        assertEquals(400, respString.statusCode(), "Bad request");
    }

    @Test
    void testDeleteMovie() throws IOException, InterruptedException {
        String idString = "asd";
        int idNoCorrect = 5;
        int idCorrect = 4;

        for (int i = 1; i < 5; i++) {
            postRequest(String.format("{\"title\": \"Властелин морей:%s\", \"year\": \"2010\"}", i));
        }
        System.out.println(getByID(3));

        HttpResponse respString = delByID(idString);
        HttpResponse respNoCorrect = delByID(idNoCorrect);
        HttpResponse respCorrect = delByID(idCorrect);


        assertEquals(204, respCorrect.statusCode(), "Фильм удален");

        assertEquals(404, respNoCorrect.statusCode(), "Фильм не должен быть найден");

        assertEquals("{\"error\":\"Ошибка ввода\",\"details\":[\"Введите ЧИСЛО.\"]}", respString.body(), "Bad request");


    }

    @Test
    void testParameterYear() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies?year=2010")).build();
        HttpResponse.BodyHandler handler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        String film1 = "{\"title\":\"Фильм - 1\", \"year\": 2010}";
        String film2 = "{\"title\":\"Фильм - 2\", \"year\": 2011}";
        String film3 = "{\"title\":\"Фильм - 3\", \"year\": 2010}";
        String film4 = "{\"title\":\"Фильм - 4\", \"year\": 2012}";

        postRequest(film1);
        postRequest(film2);
        postRequest(film3);
        postRequest(film4);

        HttpResponse response = client.send(request, handler);

        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class, new LocalDateAdapter()).create();
        List<Movie> listMovies = gson.fromJson(response.body().toString(), new ListOfMoviesTypeToken().getType());
        assertEquals(2, listMovies.size(), "В списке должно быть 2 фильма 2010 года");

        delByID(1);
        HttpResponse response1 = client.send(request, handler);
        List<Movie> listMovie = gson.fromJson(response1.body().toString(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, listMovie.size(), "В списке должно быть 1 фильм 2010 года");

        delByID(3);
        HttpResponse responseEmtyList = client.send(request, handler);

        List<Movie> emtyList = gson.fromJson(responseEmtyList.body().toString(), new ListOfMoviesTypeToken().getType());
        assertTrue(emtyList.isEmpty(), "Возвращаем пустую коллекцию");
    }

    private static <T> HttpResponse delByID(T id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().DELETE().uri(URI.create(BASE + "/movies/" + id)).build();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse resp = client.send(req, bodyHandler);

        return resp;
    }

    private static <T> HttpResponse getByID(T id) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().GET().uri(URI.create(BASE + "/movies/" + id)).build();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        HttpResponse resp = client.send(req, bodyHandler);

        return resp;
    }

    private static HttpResponse postRequest(String jsonString) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(jsonString)).header(CT, CT_VALUE).uri(URI.create(BASE + "/movies")).build();
        HttpResponse.BodyHandler<String> responseBodyHandler = HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);
        HttpResponse<String> response = client.send(request, responseBodyHandler);
        return response;
    }
}

