package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.Category;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository repository;

    @Test
    void findByName_returnCategory_whenCategoryExists() {
        final var category = new Category().setName("C4");
        repository.save(category);

        final var result = repository.findByName("C4");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("C4");
    }

    @Test
    void findByName_returnEmpty_whenCategoryDoesNotExist() {
        final var result = repository.findByName("WRONG");

        assertThat(result).isEmpty();
    }

}
