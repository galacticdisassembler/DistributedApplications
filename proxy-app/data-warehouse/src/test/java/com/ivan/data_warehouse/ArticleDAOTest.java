package com.ivan.data_warehouse;

import com.ivan.common.models.ArticleModel;
import junit.framework.Assert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import java.util.List;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArticleDAOTest {
    private static final Logger logger = LogManager.getLogger(ArticleDAOTest.class);

    private final ArticleDAO articleDao = ArticleDAO.getInstance();

    @Test
    public void insertTest() {

        ArticleModel model = new ArticleModel();
        model.setAuthorFullName("123");
        model.setCategory("234");
        model.setContent("345");
        model.setTitle("q");

        boolean inserted = articleDao.insert(new ArticleModel[] {model});
        int insertedId = model.getId();

        Assert.assertTrue(inserted);
        Assert.assertTrue(insertedId > 0);
    }

    @Test
    public void selectAndDeleteTest() {

        ArticleModel model = new ArticleModel();
        model.setAuthorFullName("123");
        model.setCategory("234");
        model.setContent("345");
        model.setTitle("q");

        List<ArticleModel> selected = articleDao.select(0, 5);

        Assert.assertTrue(selected.size() > 0);

        // boolean deleted = articleDao.delete(insertedId);

        // Assert.assertTrue(deleted);

    }
}
