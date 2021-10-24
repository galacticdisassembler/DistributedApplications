package com.ivan.common.models;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ArticleModel {

    private Integer id;

    private String authorFullName;

    private String title;

    private String content;

    private String category;

}
