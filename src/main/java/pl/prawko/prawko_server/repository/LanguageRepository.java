package pl.prawko.prawko_server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.prawko.prawko_server.model.Language;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {
}
