package pl.prawko.prawko_server.service.implementation;

import org.springframework.stereotype.Service;
import pl.prawko.prawko_server.model.Language;
import pl.prawko.prawko_server.repository.LanguageRepository;
import pl.prawko.prawko_server.service.ILanguageService;

import java.util.List;

/**
 * Implementation of {@link ILanguageService} that retrieves {@link Language} entities using a {@link LanguageRepository}.
 */
@Service
public class LanguageService implements ILanguageService {

    private final LanguageRepository repository;

    public LanguageService(final LanguageRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Language> findAll() {
        return repository.findAll();
    }

}
