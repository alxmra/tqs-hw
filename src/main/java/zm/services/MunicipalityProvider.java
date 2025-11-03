package zm.services;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface MunicipalityProvider {
    public List<String> getMunicipalities();
    public boolean isValid(String municipality);
}
