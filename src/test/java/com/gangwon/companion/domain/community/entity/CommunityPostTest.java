package com.gangwon.companion.domain.community.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CommunityPostTest {

    @Test
    @DisplayName("replaceImages keeps existing image when same s3Key is requested")
    void replaceImages_keepsExistingImage_whenSameS3KeyRequested() {
        CommunityPost post = CommunityPost.builder().title("title").content("content").build();
        CommunityPostImage existing = image(post, "community/existing.jpg", "old-url", 0);
        CommunityPostImage added = image(post, "community/added.jpg", "added-url", 2);
        post.addImage(existing);

        post.replaceImages(List.of(
                image(post, "community/existing.jpg", "new-url", 1),
                added
        ));

        assertThat(post.getImages()).containsExactly(existing, added);
        assertThat(existing.getUrl()).isEqualTo("new-url");
        assertThat(existing.getSortOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("replaceImages removes image omitted from update request")
    void replaceImages_removesImage_whenOmittedFromRequest() {
        CommunityPost post = CommunityPost.builder().title("title").content("content").build();
        CommunityPostImage kept = image(post, "community/kept.jpg", "kept-url", 0);
        CommunityPostImage removed = image(post, "community/removed.jpg", "removed-url", 1);
        post.addImage(kept);
        post.addImage(removed);

        post.replaceImages(List.of(image(post, "community/kept.jpg", "updated-url", 0)));

        assertThat(post.getImages()).containsExactly(kept);
        assertThat(kept.getUrl()).isEqualTo("updated-url");
    }

    private CommunityPostImage image(CommunityPost post, String s3Key, String url, int sortOrder) {
        return CommunityPostImage.builder()
                .post(post)
                .s3Key(s3Key)
                .url(url)
                .sortOrder(sortOrder)
                .build();
    }
}
