package pl.prawko.prawko_server.service;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pl.prawko.prawko_server.exception.UnknownCategoryException;
import pl.prawko.prawko_server.model.CategoryVariant;
import pl.prawko.prawko_server.repository.CategoryRepository;
import pl.prawko.prawko_server.service.implementation.CategoryService;
import pl.prawko.prawko_server.test_data.CategoryTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {


    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryService service;

    @Test
    void findByName_returnCategory_whenFound() {
        final var expected = CategoryTestData.CATEGORY_B;
        when(repository.findByName(expected.getName())).thenReturn(Optional.of(expected));

        final var result = service.findByName(expected.getName());

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findByName_throwEntityNotFound_whenNotFound() {
        final var name = CategoryVariant.T;
        when(repository.findByName(name)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByName(name))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Category '" + name + "' not found.");
    }

    @Test
    void findAllFromString_returnCategories_whenFound() {
        final var categories = List.of(CategoryTestData.CATEGORY_A, CategoryTestData.CATEGORY_B);
        categories.forEach(category -> when(repository.findByName(category.getName()))
                .thenReturn(Optional.of(category)));

        final var result = service.findAllFromString("A,B");

        assertThat(result).isEqualTo(categories);
    }

    @Test
    void findAllFromString_throwUnknownCategory_whenUnknownEmptyOrNotFound() {
        final var category = CategoryTestData.CATEGORY_B;
        when(repository.findByName(CategoryVariant.T)).thenReturn(Optional.empty());
        when(repository.findByName(category.getName())).thenReturn(Optional.of(category));

        final var exception = catchThrowableOfType(UnknownCategoryException.class,
                () -> service.findAllFromString("Z,T,,b,B,"));

        assertThat(exception.getNames()).isEqualTo(List.of("Z", "T", "", "b", ""));
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void findAllFromString_throwUnknownCategory_whenInputIsNullOrEmpty(final String input) {
        final var exception = catchThrowableOfType(UnknownCategoryException.class,
                () -> service.findAllFromString(input));

        assertThat(exception.getNames()).isEqualTo(List.of(""));
        verifyNoInteractions(repository);
    }

}
