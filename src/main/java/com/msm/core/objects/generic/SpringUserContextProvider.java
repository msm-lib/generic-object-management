package com.msm.core.objects.generic;

import com.msm.core.dynamicquery.UserContextProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class SpringUserContextProvider implements UserContextProvider {

    @Override
    public UUID getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
//        return ((CustomPrincipal) auth.getPrincipal()).getUserId();

        return null;
    }

    @Override
    public String getUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
}