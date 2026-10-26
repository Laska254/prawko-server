package pl.prawko.prawko_server.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.test_data.CategoryTestData;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository repository;

    @Test
    void findByName_returnCategory_whenCategoryExists() {
        final var expected = CategoryTestData.CATEGORY_PT;

        final var result = repository.findByName(expected.getName());

        assertThat(result).get()
                .extracting(Category::getId, Category::getName)
                .containsExactly(expected.getId(), expected.getName());
    }

    @Test
    void findByName_returnEmpty_whenCategoryDoesNotExist() {
        final var category = CategoryTestData.CATEGORY_B;
        repository.deleteById(category.getId());

        final var result = repository.findByName(category.getName());

        assertThat(result).isEmpty();
    }

}
