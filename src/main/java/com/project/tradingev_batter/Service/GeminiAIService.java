package com.project.tradingev_batter.Service;

import com.project.tradingev_batter.dto.PriceSuggestionRequest;
import com.project.tradingev_batter.dto.PriceSuggestionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GeminiAIService {

    @Value("${gemini.api.key:AIzaSyBZChmiQl3VV0yiYzTcW4AxQNKON9fCCgU}")
    private String apiKey;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeminiAIService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public PriceSuggestionResponse suggestPrice(PriceSuggestionRequest request) {
        try {
            // Tạo prompt cho Gemini
            String prompt = buildPrompt(request);

            // Gọi Gemini API
            String geminiResponse = callGeminiAPI(prompt);

            // Parse response và trích xuất giá
            return parseGeminiResponse(geminiResponse, request);

        } catch (Exception e) {
            System.err.println("Error calling Gemini API: " + e.getMessage());
            e.printStackTrace();
            
            // Fallback: Trả về giá ước lượng dựa trên logic đơn giản
            return getFallbackPrice(request);
        }
    }

    private String buildPrompt(PriceSuggestionRequest request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a market analyst for second-hand electric vehicles and batteries in Vietnam. ");
        prompt.append("Provide a price range estimate in Vietnamese Dong (VND) for the following product:\n\n");
        
        if ("CAR".equalsIgnoreCase(request.getProductType())) {
            prompt.append("Product Type: Electric Vehicle (Car)\n");
            prompt.append("Brand: ").append(request.getBrand()).append("\n");
            if (request.getModel() != null) {
                prompt.append("Model: ").append(request.getModel()).append("\n");
            }
            prompt.append("Year: ").append(request.getYear()).append("\n");
            prompt.append("Condition: ").append(request.getCondition()).append("\n");
            if (request.getMileage() != null) {
                prompt.append("Mileage: ").append(request.getMileage()).append(" km\n");
            }
        } else {
            prompt.append("Product Type: Electric Vehicle Battery\n");
            prompt.append("Brand: ").append(request.getBrand()).append("\n");
            prompt.append("Year: ").append(request.getYear()).append("\n");
            prompt.append("Condition: ").append(request.getCondition()).append("\n");
            if (request.getCapacity() != null) {
                prompt.append("Capacity: ").append(request.getCapacity()).append(" kWh\n");
            }
        }
        
        prompt.append("\nPlease provide:\n");
        prompt.append("1. Minimum price (VND)\n");
        prompt.append("2. Maximum price (VND)\n");
        prompt.append("3. Suggested/Average price (VND)\n");
        prompt.append("4. Brief market insight (1-2 sentences in Vietnamese)\n\n");
        prompt.append("Format your response EXACTLY as follows:\n");
        prompt.append("MIN: [number]\n");
        prompt.append("MAX: [number]\n");
        prompt.append("SUGGESTED: [number]\n");
        prompt.append("INSIGHT: [your insight here]");
        
        return prompt.toString();
    }

    private String callGeminiAPI(String prompt) throws Exception {
        String url = GEMINI_API_URL + "?key=" + apiKey;

        // Tạo request body theo format của Gemini API
        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        Map<String, String> part = new HashMap<>();
        
        part.put("text", prompt);
        content.put("parts", List.of(part));
        requestBody.put("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            entity,
            String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            // Parse JSON response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isEmpty() && candidates.isArray()) {
                JsonNode firstCandidate = candidates.get(0);
                JsonNode contentNode = firstCandidate.path("content");
                JsonNode parts = contentNode.path("parts");
                if (!parts.isEmpty() && parts.isArray()) {
                    return parts.get(0).path("text").asText();
                }
            }
        }

        throw new Exception("Invalid response from Gemini API");
    }

    private PriceSuggestionResponse parseGeminiResponse(String geminiResponse, PriceSuggestionRequest request) {
        try {
            // Parse response theo format đã định
            Double minPrice = extractPrice(geminiResponse, "MIN:");
            Double maxPrice = extractPrice(geminiResponse, "MAX:");
            Double suggestedPrice = extractPrice(geminiResponse, "SUGGESTED:");
            String insight = extractInsight(geminiResponse);

            // Validation
            if (minPrice == null || maxPrice == null || suggestedPrice == null) {
                return getFallbackPrice(request);
            }

            return new PriceSuggestionResponse(minPrice, maxPrice, suggestedPrice, insight);

        } catch (Exception e) {
            System.err.println("Error parsing Gemini response: " + e.getMessage());
            return getFallbackPrice(request);
        }
    }

    private Double extractPrice(String text, String prefix) {
        try {
            Pattern pattern = Pattern.compile(prefix + "\\s*([0-9,]+\\.?[0-9]*)");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                String priceStr = matcher.group(1).replace(",", "");
                return Double.parseDouble(priceStr);
            }
        } catch (Exception e) {
            System.err.println("Error extracting price with prefix " + prefix + ": " + e.getMessage());
        }
        return null;
    }

    private String extractInsight(String text) {
        try {
            Pattern pattern = Pattern.compile("INSIGHT:\\s*(.+?)(?=\\n\\n|$)", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        } catch (Exception e) {
            System.err.println("Error extracting insight: " + e.getMessage());
        }
        return "Không có thông tin thị trường chi tiết.";
    }

    private PriceSuggestionResponse getFallbackPrice(PriceSuggestionRequest request) {
        // Logic ước lượng giá đơn giản khi Gemini API không khả dụng
        double basePrice;
        
        if ("CAR".equalsIgnoreCase(request.getProductType())) {
            // Giá xe điện cũ ước tính
            basePrice = 300_000_000; // 300 triệu VNĐ
            
            // Điều chỉnh theo năm
            int age = 2025 - request.getYear();
            basePrice *= Math.max(0.5, 1 - (age * 0.1)); // Giảm 10%/năm
            
            // Điều chỉnh theo tình trạng
            if ("USED".equalsIgnoreCase(request.getCondition())) {
                basePrice *= 0.8;
            }
            
        } else {
            // Giá pin ước tính
            basePrice = 50_000_000; // 50 triệu VNĐ
            
            // Điều chỉnh theo năm
            int age = 2025 - request.getYear();
            basePrice *= Math.max(0.4, 1 - (age * 0.15)); // Giảm 15%/năm
            
            // Điều chỉnh theo dung lượng
            if (request.getCapacity() != null) {
                basePrice *= (request.getCapacity() / 50.0); // Scale theo capacity
            }
            
            // Điều chỉnh theo tình trạng
            if ("USED".equalsIgnoreCase(request.getCondition())) {
                basePrice *= 0.7;
            }
        }
        
        double minPrice = basePrice * 0.8;
        double maxPrice = basePrice * 1.2;
        double suggestedPrice = basePrice;
        
        String insight = "Giá ước lượng dựa trên dữ liệu thị trường chung. " +
                        "Giá thực tế có thể thay đổi tùy theo tình trạng cụ thể của sản phẩm.";
        
        return new PriceSuggestionResponse(minPrice, maxPrice, suggestedPrice, insight);
    }
}
