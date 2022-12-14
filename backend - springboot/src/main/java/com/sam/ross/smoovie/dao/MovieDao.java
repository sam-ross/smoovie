package com.sam.ross.smoovie.dao;

import com.sam.ross.smoovie.exceptions.ServiceProxyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.lang.String.format;

@Repository
@Slf4j
public class MovieDao {
    private static final String OPEN_SUBTITLES_BASE_URL = "https://api.opensubtitles.com/api/v1";

    /**
     * Calls the "search movie" endpoint in the IMDb API to retrieve the 5 most relevant matches to the title
     *
     * @param title  - Title of the movie we're trying to find
     * @param apiKey - IMDb API key
     * @return JSON response string to be used in the service to extract the IMDb ID
     */
    public String searchIMDbMovies(String title, String apiKey) {
        log.info("Sending request to the IMDb 'searchMovie' endpoint: [{}]", title);

        String convertedMovieName = title.replace(" ", "%20");
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(format("https://imdb-api.com/en/API/SearchMovie/%s/%s", apiKey, convertedMovieName)))
                .build();

        HttpResponse<String> response = getHttpResponse(client, request);
        log.info("Response returned successfully from IMDb API");
        log.trace("Received response from the IMDb 'searchMovie' endpoint: [{}]", response.body());

        return response.body();
    }

    /**
     * Calls the "search for subtitles" endpoint in the OpenSubtitles API to retrieve a list of different subtitle
     * files they have for that movie
     *
     * @param imdbId - The IMDb ID is used to specify which movie we are requesting subtitles for
     * @param apiKey - OpenSubtitles API key
     * @return JSON response string to be used in the service to extract the unique OpenSubtitles file ID for
     * that particular subtitle file
     */
    public String searchForSubtitles(String imdbId, String apiKey) {
        log.info("Sending request to the OpenSubtitles 'searchSubtitles' endpoint: [{}]", imdbId);

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .header("Api-Key", apiKey)
                .uri(URI.create(OPEN_SUBTITLES_BASE_URL + "/subtitles?languages=en&imdb_id=" + imdbId))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = getHttpResponse(client, request);
        log.info("Response returned successfully from OpenSubtitles API");
        log.trace("Received response from the OpenSubtitles 'searchSubtitles' endpoint: [{}]", response.body());

        return response.body();
    }

    /**
     * Calls the "login" endpoint in the OpenSubtitles API to log in with my account. A bearer token is included
     * in the response, which is needed later, for calling the "download subtitles" endpoint
     *
     * @param apiKey   - OpenSubtitles API key
     * @param username - OpenSubtitles username
     * @param password - OpenSubtitles password
     * @return JSON response string to be used in the service to extract the bearer token which will be used later
     * to call the "download subtitles" endpoint
     */
    public String logInAccount(String apiKey, String username, String password) {
        log.info("Sending request to the OpenSubtitles 'logIn' endpoint");
        String requestBody = format("{\"password\": \"%s\", \"username\": \"%s\"}", password, username);

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPEN_SUBTITLES_BASE_URL + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Api-Key", apiKey)
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = getHttpResponse(client, request);
        log.info("Response returned successfully from OpenSubtitles API");
        log.trace("Received response from the OpenSubtitles 'logIn' endpoint");

        return response.body();
    }

    /**
     * Calls the "download subtitles" endpoint in the OpenSubtitles API to retrieve a download link which will
     * be used later to download the subtitles SRT file
     *
     * @param fileId - The unique OpenSubtitles file ID to specify which subtitle file we are requesting the
     *               download link for
     * @param apiKey - OpenSubtitles API key
     * @return JSON response string to be used in the service to extract the actual download link which will be
     * used later to download the subtitles file
     */
    public String requestForDownload(String fileId, String apiKey, String bearerToken) {
        log.info("Sending request to the OpenSubtitles 'download' endpoint: [{}]", fileId);

        String requestBody = format("{\"file_id\": %s}", fileId);
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OPEN_SUBTITLES_BASE_URL + "/download"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .header("Authorization", format("Bearer %s", bearerToken))
                .header("Api-Key", apiKey)
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = getHttpResponse(client, request);
        log.info("Response returned successfully from OpenSubtitles API");
        log.trace("Received response from the OpenSubtitles 'downloadSubtitles' endpoint: [{}]", response.body());

        return response.body();
    }

    /**
     * Calls the actual download link for the subtitles SRT file which will be used later to extract all the words
     *
     * @param downloadLink - The download link of the SRT file we are trying to download from OpenSubtitles
     * @return JSON response string containing the SRT file contents of the OpenSubtitles file
     */
    public String useDownloadLink(String downloadLink) {
        log.info("Sending request to the OpenSubtitles download link: [{}]", downloadLink);

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadLink))
                .build();

        HttpResponse<String> response = getHttpResponse(client, request);
        log.info("Response returned successfully from OpenSubtitles API");
        log.trace("Received response from the OpenSubtitles download link: [{}]", response.body());

        return response.body();
    }

    /**
     * Executes the HTTP request out to the respective client, whether that's the IMDb API or the OpenSubtitles API
     *
     * @param client  - The client we are making the HTTP request out to
     * @param request - The request that we are sending to the HTTP client
     * @return The HTTP response returned from the respective client
     */
    private HttpResponse<String> getHttpResponse(HttpClient client, HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
        } catch (Exception e) {
            throw ServiceProxyException.builder()
                    .httpStatus(502)
                    .message("Http request to client failed")
                    .build();
        }

        if (response.statusCode() != 200) {
            log.debug(format("Unexpected status code: %d", response.statusCode()));
            throw ServiceProxyException.builder()
                    .httpStatus(response.statusCode())
                    .message(response.body())
                    .build();
        }

        return response;
    }
}
