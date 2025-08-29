package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@SpringBootApplication
public class TestApplication implements CommandLineRunner {

    private final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        SpringApplication.run(TestApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");        
        requestBody.put("regNo", "REG12346");       
        requestBody.put("email", "john@example.com"); 

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                generateWebhookUrl,
                HttpMethod.POST,
                entity,
                new ParameterizedTypeReference<Map<String, Object>>() {}
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> body = response.getBody();

            String webhookUrl = Optional.ofNullable(body.get("webhook"))
                                        .map(Object::toString)
                                        .orElse(null);

            String accessToken = Optional.ofNullable(body.get("accessToken"))
                                         .map(Object::toString)
                                         .orElse(null);

            if (webhookUrl == null || accessToken == null) {
                System.out.println("Missing webhook or accessToken in response.");
                return;
            }

            System.out.println("Webhook URL: " + webhookUrl);
            System.out.println("Access Token: " + accessToken);

            String finalSqlQuery =
                    "SELECT " +
                    "    e1.EMP_ID, " +
                    "    e1.FIRST_NAME, " +
                    "    e1.LAST_NAME, " +
                    "    d.DEPARTMENT_NAME, " +
                    "    COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
                    "FROM EMPLOYEE e1 " +
                    "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
                    "LEFT JOIN EMPLOYEE e2 " +
                    "    ON e1.DEPARTMENT = e2.DEPARTMENT " +
                    "   AND e2.DOB > e1.DOB " +
                    "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
                    "ORDER BY e1.EMP_ID DESC;";

            HttpHeaders authHeaders = new HttpHeaders();
            authHeaders.setContentType(MediaType.APPLICATION_JSON);
            authHeaders.set("Authorization", accessToken); // FIXED: no "Bearer"

            Map<String, String> sqlBody = new HashMap<>();
            sqlBody.put("finalQuery", finalSqlQuery);

            HttpEntity<Map<String, String>> sqlEntity = new HttpEntity<>(sqlBody, authHeaders);
            ResponseEntity<String> sqlResponse = restTemplate.postForEntity(webhookUrl, sqlEntity, String.class);

            System.out.println("Submission Response: " + sqlResponse.getBody());
        } else {
            System.out.println("Failed to generate webhook.");
        }
    }
}
