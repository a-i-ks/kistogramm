package de.iske.kistogramm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "kistogramm")
public class DefaultCategoryConfig {

    private List<CategoryInit> defaultCategories;

    public List<CategoryInit> getDefaultCategories() {
        return defaultCategories;
    }

    public void setDefaultCategories(List<CategoryInit> defaultCategories) {
        this.defaultCategories = defaultCategories;
    }

    public static class CategoryInit {
        private String name;
        private List<String> attributes;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getAttributes() {
            return attributes;
        }

        public void setAttributes(List<String> attributes) {
            this.attributes = attributes;
        }
    }
}
