package ru.pol;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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
    private AtomicInteger requestCount;
    private AtomicLong lastRequestTime;

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
                    wait(timeUnit.toMillis(1) - timeDiff);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
    }

    private String convertToJSON(Document doc) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        return mapper.writeValueAsString(doc);
    }

    public static void main(String[] args) {

        CrptApi api = new CrptApi(TimeUnit.SECONDS, 1L, 10);
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionDate;
        private String productionType;
        private List<Product> products;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate regDate;
        private String regNumber;

        @JsonGetter("doc_id")
        public String getDocId() {
            return docId;
        }

        @JsonGetter("doc_status")
        public String getDocStatus() {
            return docStatus;
        }

        @JsonGetter("doc_type")
        public String getDocType() {
            return docType;
        }

        @JsonGetter("owner_inn")
        public String getOwnerInn() {
            return ownerInn;
        }

        @JsonGetter("participant_inn")
        public String getParticipantInn() {
            return participantInn;
        }

        @JsonGetter("producer_inn")
        public String getProducerInn() {
            return producerInn;
        }

        @JsonGetter("production_date")
        public LocalDate getProductionDate() {
            return productionDate;
        }

        @JsonGetter("production_type")
        public String getProductionType() {
            return productionType;
        }

        @JsonGetter("reg_date")
        public LocalDate getRegDate() {
            return regDate;
        }

        @JsonGetter("reg_number")
        public String getRegNumber() {
            return regNumber;
        }
    }

    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        private String certificateDocument;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate certificateDocumentDate;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;

        @JsonGetter("certificate_document")
        public String getCertificateDocument() {
            return certificateDocument;
        }

        @JsonGetter("certificate_document_date")
        public LocalDate getCertificateDocumentDate() {
            return certificateDocumentDate;
        }

        @JsonGetter("certificate_document_number")
        public String getCertificateDocumentNumber() {
            return certificateDocumentNumber;
        }

        @JsonGetter("owner_inn")
        public String getOwnerInn() {
            return ownerInn;
        }

        @JsonGetter("producer_inn")
        public String getProducerInn() {
            return producerInn;
        }

        @JsonGetter("production_date")
        public LocalDate getProductionDate() {
            return productionDate;
        }

        @JsonGetter("tnved_code")
        public String getTnvedCode() {
            return tnvedCode;
        }

        @JsonGetter("uit_code")
        public String getUitCode() {
            return uitCode;
        }

        @JsonGetter("uitu_code")
        public String getUituCode() {
            return uituCode;
        }
    }

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Description {
        private String participantInn;
    }
}
