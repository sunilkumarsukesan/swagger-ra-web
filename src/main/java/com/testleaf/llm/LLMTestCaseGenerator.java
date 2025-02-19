package com.testleaf.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LLMTestCaseGenerator {

    @Value("${llm.api.url}")
    private String llmApiUrl;

    @Value("${llm.api.key}")
    private String apiKey;

    @Value("${llm.model}")
    private String modelName;

    /**
     * Generates test cases given user story details and test types.
     */

    public String llmGenerateTestCases(String testType, String userStoryDescription, String applicationUrl,
                                    String acceptanceCriteria, String epicDescription) {

        if (userStoryDescription == null || userStoryDescription.isEmpty()) {
            return "No valid user story description to generate test cases.";
        }

        // Build the test type instruction line
        String testTypeLine;
        if (testType == null || testType.isEmpty()) {
            testTypeLine = "- Include Only Positive tests\n";
        } else if ("negative".equalsIgnoreCase(testType)) {
            testTypeLine = "- Include Only Negative tests\n";
        } else if ("edge".equalsIgnoreCase(testType)) {
            testTypeLine = "- Include Only Edge tests\n";
        } else {
            testTypeLine = "- Include Only Positive tests\n";
        }

        // Build the user prompt using the user story details
        String userPrompt = "Generate test cases for the following user story description:\n"
                + userStoryDescription + "\n"
                + "Application URL: " + applicationUrl + "\n"
                + "Acceptance Criteria: " + acceptanceCriteria;

        if (epicDescription != null && !epicDescription.isEmpty()) {
            userPrompt += "\nEpic Description: " + epicDescription;
        }

        try {
            // Prepare the chat messages
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");

            String systemContent = "You are a helpful assistant that generates **manual test cases** for a user story.\n"
                    + "Your response must contain only test case steps written in plain English, with clear instructions.\n"
                    + "Do not include any code or automated test steps. Write them in a step-by-step format.\n\n"
                    + "- Each test case should cover different scenarios like positive, negative, and edge cases.\n"
                    + "- Include appropriate preconditions, actions, and expected results for each test case.\n"
                    + "- The test cases should be comprehensive, covering all possible conditions for the user story.\n"
                    + "- Provide test case IDs for easy reference.\n"
                    + "- The test cases should be applicable for manual testing, no automation details should be included.\n"
                    + "- Write the test case steps as actions to be performed manually by the tester.\n\n"
                    + "**[IMPORTANT]**\n"
                    + "Your response should be in **structured JSON format** as shown below:\n\n"
                    + "{\n"
                    + "  \"testCases\": [\n"
                    + "    {\n"
                    + "      \"TC_No\": \"TC001\",\n"
                    + "      \"Test_Case_Description\": \"Verify login with valid credentials\",\n"
                    + "      \"Pre-requisites\": \"User must have valid credentials\",\n"
                    + "      \"Test_Steps\": \"1. Open login page\\n2. Enter valid username\\n3. Enter valid password\\n4. Click on Login\",\n"
                    + "      \"Actual_Result\": \"User is successfully logged in\",\n"
                    + "      \"Expected_Result\": \"User should be logged in successfully\",\n"
                    + "      \"Status\": \"Pending\"\n"
                    + "    },\n"
                    + "    {\n"
                    + "      \"TC_No\": \"TC002\",\n"
                    + "      \"Test_Case_Description\": \"Verify login with invalid credentials\",\n"
                    + "      \"Pre-requisites\": \"User must have an invalid set of credentials\",\n"
                    + "      \"Test_Steps\": \"1. Open login page\\n2. Enter invalid username\\n3. Enter invalid password\\n4. Click on Login\",\n"
                    + "      \"Actual_Result\": \"Error message displayed\",\n"
                    + "      \"Expected_Result\": \"User should receive an error message\",\n"
                    + "      \"Status\": \"Pending\"\n"
                    + "    }\n"
                    + "  ]\n"
                    + "}";



            systemMessage.put("content", systemContent);
            messages.add(systemMessage);

            // Prepare the user message with user story details
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);

            // Build the request payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("model", modelName);
            payload.put("messages", messages);
            payload.put("temperature", 0.1);
            payload.put("top_p", 0.2);
            payload.put("max_tokens", 10000);

            ObjectMapper mapper = new ObjectMapper();
            String requestBody = mapper.writeValueAsString(payload);

            // Call the LLM endpoint
            return callLLMApi(requestBody);

        } catch (Exception e) {
            e.printStackTrace();
            return "Error building JSON payload: " + e.getMessage();
        }
    }

    // For backward compatibility: defaults to positive tests if testType is not provided.
    public String generateTestCases(String userStoryDescription, String applicationUrl,
                                    String acceptanceCriteria, String epicDescription) {
        return llmGenerateTestCases("positive", userStoryDescription, applicationUrl, acceptanceCriteria, epicDescription);
    }

    private String callLLMApi(String requestBody) {
        try (var httpClient = HttpClients.createDefault()) {
            var request = new HttpPost(llmApiUrl);
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Authorization", "Bearer " + apiKey);
            request.setEntity(new StringEntity(requestBody));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling LLM API: " + e.getMessage();
        }
    }
}
