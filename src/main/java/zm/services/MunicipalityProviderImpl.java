package zm.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class MunicipalityProviderImpl implements MunicipalityProvider {
    private static final Logger logger = LoggerFactory.getLogger(MunicipalityProviderImpl.class);
    private static final String API_URL = "https://gist.githubusercontent.com/alxmra/f6ddfdcefcbbb2112704fff57ba2baf0/raw/9632d6ade1e08d857a3395854c268dd3f9e96f6b/municipalities.json";
    private final List<String> municipalities = fetchMunicipalities();

    public List<String> getMunicipalities() {
        return new ArrayList<>(this.municipalities);
    }

    public boolean isValid(String municipality) {
        if (municipality == null || municipality.isBlank()) {
            return false;
        }

        List<String> validMunicipalities = getMunicipalities();
        return validMunicipalities.contains(municipality);
    }

    private List<String> fetchMunicipalities() {
        try (HttpClient client = HttpClient.newHttpClient()){
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .build();

            HttpResponse<String> response = client.send(
                    request, HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() == 200) {
                String jsonResponse = response.body();
                return jsonToList(jsonResponse);

            } else {
                logger.error("HTTP Error: {}", response.statusCode());
                return List.of();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while fetching municipalities", e);
            return List.of();
        } catch (Exception e) {
            logger.error("Error fetching municipalities", e);
            return List.of();
        }
    }

    private static List<String> jsonToList(String jsonString) {
        try {
            List<String> result = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                result.add(jsonArray.getString(i));
            }

            return result;

        } catch (JSONException e) {
            logger.error("Error parsing JSON array", e);
            return new ArrayList<>();
        }
    }
}
