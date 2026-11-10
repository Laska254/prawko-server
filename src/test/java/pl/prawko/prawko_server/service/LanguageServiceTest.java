package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.repository.LanguageRepository;
import pl.prawko.prawko_server.service.implementation.LanguageService;
import pl.prawko.prawko_server.testdata.LanguageTestData;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    @Mock
    private LanguageRepository repository;

    @InjectMocks
    private LanguageService service;

    @Test
    void findAll_returnLanguages_whenFound() {
        final var languages = LanguageTestData.ALL;
        when(repository.findAll()).thenReturn(languages);

        final var result = service.findAll();

        assertThat(result).isEqualTo(languages);
    }

    @Test
    void findAll_returnEmptyList_whenNotFound() {
        when(repository.findAll()).thenReturn(Collections.emptyList());

        final var result = service.findAll();

        assertThat(result).isEmpty();
    }

}
