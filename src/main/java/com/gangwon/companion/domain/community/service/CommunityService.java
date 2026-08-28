package com.gangwon.companion.domain.community.service;

import com.gangwon.companion.domain.community.dto.CommunityDtos.*;
import com.gangwon.companion.domain.community.entity.*;
import com.gangwon.companion.domain.community.repository.*;
import com.gangwon.companion.domain.course.entity.SavedCourse;
import com.gangwon.companion.domain.course.repository.SavedCourseRepository;
import com.gangwon.companion.domain.user.entity.User;
import com.gangwon.companion.domain.user.repository.UserRepository;
import com.gangwon.companion.global.exception.*;
import com.gangwon.companion.global.storage.S3FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Set;

@Service @RequiredArgsConstructor
public class CommunityService {
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityPostLikeRepository likeRepository;
    private final CommunityPostSaveRepository saveRepository;
    private final CommunityCommentLikeRepository commentLikeRepository;
    private final UserRepository userRepository;
    private final SavedCourseRepository courseRepository;
    private final S3FileService s3FileService;

    @Transactional(readOnly = true)
    public Page<PostSummary> list(String username, String keyword, Pageable pageable) {
        return postRepository.search(keyword == null || keyword.isBlank() ? null : keyword, pageable).map(p -> summary(p, username));
    }
    @Transactional
    public PostDetail get(String username, Long id) {
        CommunityPost post = post(id); post.increaseViewCount();
        return detail(post, username);
    }
    @Transactional
    public PostDetail create(String username, CreatePostRequest request) { return detail(save(username, request.title(), request.content(), request.courseId(), request.hashtags(), request.images()), username); }
    @Transactional
    public PostDetail update(String username, Long id, UpdatePostRequest request) {
        CommunityPost post = post(id); owner(post, username);
        post.update(request.title(), request.content(), course(request.courseId(), username), request.hashtags());
        post.replaceImages(toImages(post, request.images()));
        return detail(post, username);
    }
    @Transactional
    public void delete(String username, Long id) { CommunityPost post = post(id); owner(post, username); postRepository.delete(post); }
    @Transactional
    public CommentResponse comment(String username, Long id, CreateCommentRequest request) {
        CommunityPost post = post(id); User user = user(username);
        return commentResponse(commentRepository.save(CommunityComment.builder().post(post).user(user).content(request.content()).build()));
    }
    @Transactional
    public void like(String username, Long id) {
        CommunityPost post = post(id); User user = user(username);
        if (!likeRepository.existsByPostIdAndUserId(id, user.getId())) { likeRepository.save(CommunityPostLike.builder().post(post).user(user).build()); post.increaseLikeCount(); }
    }
    @Transactional
    public void unlike(String username, Long id) {
        CommunityPost post = post(id); User user = user(username);
        if (likeRepository.existsByPostIdAndUserId(id, user.getId())) { likeRepository.deleteByPostIdAndUserId(id, user.getId()); post.decreaseLikeCount(); }
    }
    private CommunityPost save(String username, String title, String content, Long courseId, Set<String> hashtags, List<ImageRequest> images) {
        CommunityPost post = postRepository.save(CommunityPost.builder().user(user(username)).course(course(courseId, username)).title(title).content(content).hashtags(hashtags).build()); addImages(post, images); return post;
    }
    private void addImages(CommunityPost post, List<ImageRequest> images) { if (images != null) images.forEach(i -> post.addImage(CommunityPostImage.builder().post(post).s3Key(i.s3Key()).url(i.url()).sortOrder(i.sortOrder()).build())); }
    private List<CommunityPostImage> toImages(CommunityPost post, List<ImageRequest> images) { return images == null ? null : images.stream().map(i -> CommunityPostImage.builder().post(post).s3Key(i.s3Key()).url(i.url()).sortOrder(i.sortOrder()).build()).toList(); }
    private SavedCourse course(Long id, String username) { if (id == null) return null; SavedCourse c = courseRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); if (!c.getUser().getUsername().equals(username)) throw new BusinessException(ErrorCode.ACCESS_DENIED); return c; }
    private User user(String username) { return userRepository.findByUsername(username).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private CommunityPost post(Long id) { return postRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private void owner(CommunityPost p, String username) { if (!p.getUser().getUsername().equals(username)) throw new BusinessException(ErrorCode.ACCESS_DENIED); }
    private PostSummary summary(CommunityPost p, String username) { return new PostSummary(p.getId(), p.getTitle(), p.getUser().getNickname(), isMine(p, username), isLiked(p, username), isSaved(p, username), p.getViewCount(), p.getLikeCount(), saveRepository.countByPostId(p.getId()), p.getImages().size(), p.getCourse() == null ? null : p.getCourse().getId(), p.getHashtags(), p.getCreatedAt()); }
    private PostDetail detail(CommunityPost p, String username) { List<ImageResponse> images = p.getImages().stream().map(i -> new ImageResponse(i.getS3Key(), s3FileService.createDownloadUrl(i.getS3Key()), i.getSortOrder())).toList(); List<CommentResponse> comments = commentRepository.findAllByPostIdOrderByCreatedAtAsc(p.getId()).stream().map(c -> commentResponse(c, username)).toList(); return new PostDetail(p.getId(), p.getTitle(), p.getContent(), p.getUser().getNickname(), isMine(p, username), isLiked(p, username), isSaved(p, username), p.getViewCount(), p.getLikeCount(), saveRepository.countByPostId(p.getId()), p.getCourse() == null ? null : p.getCourse().getId(), p.getHashtags(), p.getCreatedAt(), p.getUpdatedAt(), images, comments); }
    private boolean isMine(CommunityPost p, String username) { return username != null && p.getUser().getUsername().equals(username); }
    private boolean isLiked(CommunityPost p, String username) { return username != null && likeRepository.existsByPostIdAndUserId(p.getId(), user(username).getId()); }
    private boolean isSaved(CommunityPost p, String username) { return username != null && saveRepository.existsByPostIdAndUserId(p.getId(), user(username).getId()); }
    @Transactional public void save(String username, Long id) { CommunityPost post = post(id); User user = user(username); if (!saveRepository.existsByPostIdAndUserId(id, user.getId())) saveRepository.save(CommunityPostSave.builder().post(post).user(user).build()); }
    @Transactional public void unsave(String username, Long id) { User user = user(username); if (saveRepository.existsByPostIdAndUserId(id, user.getId())) saveRepository.deleteByPostIdAndUserId(id, user.getId()); }
    @Transactional public CommentResponse updateComment(String username, Long id, CreateCommentRequest request) { CommunityComment comment = comment(id); owner(comment, username); comment.update(request.content()); return commentResponse(comment, username); }
    @Transactional public void deleteComment(String username, Long id) { CommunityComment comment = comment(id); owner(comment, username); commentRepository.delete(comment); }
    @Transactional public void likeComment(String username, Long id) { CommunityComment comment = comment(id); User user = user(username); if (!commentLikeRepository.existsByCommentIdAndUserId(id, user.getId())) { commentLikeRepository.save(CommunityCommentLike.builder().comment(comment).user(user).build()); comment.increaseLikeCount(); } }
    @Transactional public void unlikeComment(String username, Long id) { CommunityComment comment = comment(id); User user = user(username); if (commentLikeRepository.existsByCommentIdAndUserId(id, user.getId())) { commentLikeRepository.deleteByCommentIdAndUserId(id, user.getId()); comment.decreaseLikeCount(); } }
    private CommunityComment comment(Long id) { return commentRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)); }
    private void owner(CommunityComment c, String username) { if (!c.getUser().getUsername().equals(username)) throw new BusinessException(ErrorCode.ACCESS_DENIED); }
    private CommentResponse commentResponse(CommunityComment c) { return commentResponse(c, null); }
    private CommentResponse commentResponse(CommunityComment c, String username) { return new CommentResponse(c.getId(), c.getUser().getNickname(), c.getContent(), c.getLikeCount(), username != null && commentLikeRepository.existsByCommentIdAndUserId(c.getId(), user(username).getId()), username != null && c.getUser().getUsername().equals(username), c.getCreatedAt()); }
}
