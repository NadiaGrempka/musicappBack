package org.musicapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:C:/Users/nictu/IdeaProjects/BackendMusicApp/music/")
                .addResourceLocations("file:C:/Users/nictu/IdeaProjects/BackendMusicApp/albumImg/");
    }
}
