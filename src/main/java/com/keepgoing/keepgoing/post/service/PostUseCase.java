package com.keepgoing.keepgoing.post.service;

import com.keepgoing.keepgoing.post.domain.Post;
import com.keepgoing.keepgoing.post.service.dto.CreatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UpdatePostCommand;
import com.keepgoing.keepgoing.post.service.dto.UserPostQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PostUseCase {

    Post createPost(CreatePostCommand command);

    Post getPost(Long postId);

    Page<Post> getMyPosts(UserPostQuery query);

    Post updatePost(UpdatePostCommand command);

    void deletePost(Long authorId, Long postId);
}
