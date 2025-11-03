package zm.services;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MunicipalityProviderTest {

    @Mock
    static MunicipalityProvider provider;

    @BeforeAll
    static void setup() {
        provider = mock(MunicipalityProvider.class);
    }

    @Test
    void testMockedProviderReturnsListAndValidates() {
        List<String> mockList = List.of("Aveiro", "Lisboa");

        when(provider.getMunicipalities()).thenReturn(mockList);
        when(provider.isValid(anyString())).thenAnswer(inv -> mockList.contains((String)inv.getArgument(0)));

        List<String> municipalities = provider.getMunicipalities();
        assertEquals(2, municipalities.size());
        assertTrue(provider.isValid("Aveiro"));
        assertFalse(provider.isValid("aveiro"));
        assertFalse(provider.isValid("Porto"));
    }

    @Test
    void testMockedProviderEmptyList() {
        List<String> empty = List.of();

        when(provider.getMunicipalities()).thenReturn(empty);
        when(provider.isValid(anyString())).thenReturn(false);

        assertNotNull(provider.getMunicipalities());
        assertTrue(provider.getMunicipalities().isEmpty());
        assertFalse(provider.isValid("Aveiro"));
    }
}