package ru.pol;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final int requestLimit;
    private AtomicInteger requestCount;
    private AtomicLong lastRequestTime;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.lastRequestTime = new AtomicLong(System.currentTimeMillis());
    }

    public void createDocument(Document doc, String signature) throws JsonProcessingException {

            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastRequestTime.get();

            if (timeDiff >= timeUnit.toMillis(1)) {
                requestCount.set(0);
                lastRequestTime.set(currentTime);
            }

            if (requestCount.get() < requestLimit) {
                requestCount.getAndIncrement();

                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder().
                        uri(URI.create("https://ismp.crpt.ru/api/v3/lk/documents/create")).
                        POST(HttpRequest.BodyPublishers.ofString(convertToJSON(doc))).build();

                try {
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    System.out.println("The status of the response obtained: " + response.statusCode());

                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    wait(timeUnit.toMillis(1) - timeDiff);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
    }

    private String convertToJSON(Document doc) throws JsonProcessingException {
        ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
        return ow.writeValueAsString(doc);
    }

    public static void main(String[] args) {

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 10);
    }

    public static class Document {
        private Description description;
        private String doc_id;
        private String doc_status;
        private String doc_type;
        private boolean importRequest;
        private String owner_inn;
        private String participant_inn;
        private String producer_inn;
        private String production_date;
        private String production_type;
        private List<Product> products;
        private String reg_date;
        private String reg_number;

        // getters and setters
    }

    public static class Product {
        private String certificate_document;
        private String certificate_document_date;
        private String certificate_document_number;
        private String owner_inn;
        private String producer_inn;
        private String production_date;
        private String tnved_code;
        private String uit_code;
        private String uitu_code;

        // getters and setters
    }

    public static class Description {
        private String participantInn;

        public Description(String participantInn) {
            this.participantInn = participantInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public void setParticipantInn(String participantInn) {
            this.participantInn = participantInn;
        }
    }
}
