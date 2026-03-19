package pl.prawko.prawko_server.service;

import pl.prawko.prawko_server.model.Language;

import java.util.List;

/**
 * Service interface for retrieving {@link Language} entities.
 * <p>
 * Provides operations to fetch all available languages used in the application.
 * </p>
 */
public interface ILanguageService {

    /**
     * Returns list of all existing {@link Language} entities.
     *
     * @return a list of {@code Language}
     */
    List<Language> findAll();

}
