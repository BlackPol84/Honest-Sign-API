package ru.pol;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CrptApi {

    private final TimeUnit timeUnit;
    private final long duration;
    private final int requestLimit;
    private final AtomicInteger requestCount;
    private final AtomicLong lastRequestTime;

    public CrptApi(TimeUnit timeUnit, long duration, int requestLimit) {
        this.timeUnit = timeUnit;
        this.duration = duration;
        this.requestLimit = requestLimit;
        this.requestCount = new AtomicInteger(0);
        this.lastRequestTime = new AtomicLong(System.currentTimeMillis());
    }

    public void createDocument(Document doc, String signature) throws JsonProcessingException {

            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - lastRequestTime.get();

            if (timeDiff >= timeUnit.toMillis(duration)) {
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
                    Thread.sleep(timeUnit.toMillis(duration) - timeDiff);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                createDocument(doc, signature);
            }
    }

    private String convertToJSON(Document doc) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return mapper.writeValueAsString(doc);
    }

    public static void main(String[] args) {

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 5L, 1);

        Document document1 = new Document(new Description("name1"), "name1", "name1",
                "name1", true, "name1", "name1", "name1",
                LocalDate.of(2024, 6, 29), "name1",
                List.of(new Product("name1", LocalDate.of(2024, 6, 29), "name1",
                        "name1", "name1", LocalDate.of(2024, 6, 29), "name1",
                        "name1", "name1")), LocalDate.of(2024, 6, 29), "name1");

        Document document2 = new Document(new Description("name2"), "name2", "name2",
                "name2", true, "name2", "name2", "name2",
                LocalDate.of(2024, 6, 29), "name2",
                List.of(new Product("name2", LocalDate.of(2024, 6, 29), "name1",
                        "name2", "name2", LocalDate.of(2024, 6, 29), "name1",
                        "name2", "name2")), LocalDate.of(2024, 6, 29), "name1");

        Document document3 = new Document(new Description("name3"), "name3", "name3",
                "name3", true, "name3", "name3", "name3",
                LocalDate.of(2024, 6, 29), "name3",
                List.of(new Product("name3", LocalDate.of(2024, 6, 29), "name1",
                        "name3", "name3", LocalDate.of(2024, 6, 29), "name1",
                        "name3", "name3")), LocalDate.of(2024, 6, 29), "name1");

        Runnable rn1 = () -> {
            try {
                api.createDocument(document1, "signature1");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };

        Runnable rn2 = () -> {
            try {
                api.createDocument(document2, "signature2");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };

        Runnable rn3 = () -> {
            try {
                api.createDocument(document3, "signature3");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        };

        new Thread(rn1, "thread1").start();
        new Thread(rn2, "thread2").start();
        new Thread(rn3, "thread3").start();
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private Description description;
        @JsonProperty("doc_id")
        private String docId;
        @JsonProperty("doc_status")
        private String docStatus;
        @JsonProperty("doc_type")
        private String docType;
        @JsonProperty("importRequest")
        private boolean importRequest;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("participant_inn")
        private String participantInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionDate;
        @JsonProperty("production_type")
        private String productionType;
        @JsonProperty("products")
        private List<Product> products;
        @JsonProperty("reg_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate regDate;
        @JsonProperty("reg_number")
        private String regNumber;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        @JsonProperty("certificate_document")
        private String certificateDocument;
        @JsonProperty("certificate_document_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate certificateDocumentDate;
        @JsonProperty("certificate_document_number")
        private String certificateDocumentNumber;
        @JsonProperty("owner_inn")
        private String ownerInn;
        @JsonProperty("producer_inn")
        private String producerInn;
        @JsonProperty("production_date")
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionDate;
        @JsonProperty("tnved_code")
        private String tnvedCode;
        @JsonProperty("uit_code")
        private String uitCode;
        @JsonProperty("uitu_code")
        private String uituCode;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }
}
