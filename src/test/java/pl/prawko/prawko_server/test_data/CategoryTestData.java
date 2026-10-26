package pl.prawko.prawko_server.test_data;

import pl.prawko.prawko_server.model.Category;
import pl.prawko.prawko_server.model.CategoryVariant;

public class CategoryTestData {

    public static final Category CATEGORY_A = new Category().setId(1L).setName(CategoryVariant.A);
    public static final Category CATEGORY_B = new Category().setId(5L).setName(CategoryVariant.B);
    public static final Category CATEGORY_PT = new Category().setId(12L).setName(CategoryVariant.PT);

}
