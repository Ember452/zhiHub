package com.solis.profile.service;

import com.solis.profile.api.dto.ProfilePatchRequest;
import com.solis.profile.api.dto.ProfileResponse;
import com.solis.user.domain.User;

import java.util.Optional;

/**
 * 个人资料业务接口。
 */
public interface ProfileService {

    Optional<User> getById(long userId);

    ProfileResponse updateProfile(long userId, ProfilePatchRequest req);

    ProfileResponse updateAvatar(long userId, String avatarUrl);
}