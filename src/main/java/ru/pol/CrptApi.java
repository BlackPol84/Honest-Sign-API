package ru.pol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private volatile int requestCount;
    private volatile long lastRequestTime;
    private final ReentrantLock locker = new ReentrantLock();

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = 0;
        this.lastRequestTime = System.currentTimeMillis();
    }

    public void creatDocument(Object document, String signature) throws JsonProcessingException {

        locker.lock();
        try {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastRequestTime;

            if (timeDiff >= timeUnit.toMillis(1)) {
                requestCount = 0;
                lastRequestTime = currentTime;
            }

            if (requestCount < requestLimit) {

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder().
                        uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create")).
                        POST(HttpRequest.BodyPublishers.ofString(convertToJSON(document))).build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("The status of the response obtained: " + response.statusCode());

                    requestCount++;

                } catch (IOException | InterruptedException e) {
                    e.getStackTrace();
                }
            } else {
                try {
                    wait(timeUnit.toMillis(1) - timeDiff);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            locker.unlock();
        }
    }

    private String convertToJSON(Object document) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(document);
    }
}
