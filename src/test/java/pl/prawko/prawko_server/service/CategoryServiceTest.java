package pl.prawko.prawko_server.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.repository.CategoryRepository;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.test_data.CategoryTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {


    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryService service;

    @Test
    void findByName_returnCategory_whenFound() {
        final var name = "B5";
        final var expected = new Category()
                .setId(5L)
                .setName(name);
        when(repository.findByName(name)).thenReturn(Optional.of(expected));

        final var result = service.findByName(name);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findAllFromString_returnCategories_whenFound() {
        final var categories = List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B);
        when(repository.findByName("A")).thenReturn(Optional.of(categories.getFirst()));
        when(repository.findByName("B")).thenReturn(Optional.of(categories.getLast()));

        final var result = service.findAllFromString("A,B");

        assertThat(result).isEqualTo(categories);
    }

    @Test
    void findAllFromString_skipCategories_whenNotFound() {
        final var category = CategoryTestData.CATEGORY_B;
        when(repository.findByName("Z")).thenReturn(Optional.empty());
        when(repository.findByName("B")).thenReturn(Optional.of(category));

        final var result = service.findAllFromString("Z,B");

        assertThat(result).isEqualTo(List.of(category));
    }

}
