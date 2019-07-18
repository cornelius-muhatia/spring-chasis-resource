package com.cm.projects.spring.resource.chasis.utils.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidTokenException;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Copyright (C) 2019 Cornelius M. & Tulaa
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Cornelius M.
 * @version 1.0.0
 */
public class MultResourceServerTokenServices implements ResourceServerTokenServices {

    /**
     * Resource server token services
     */
    private ResourceServerTokenServices[] services;
    /**
     * Logger handler
     */
    private final Logger log;


    public MultResourceServerTokenServices(ResourceServerTokenServices... services){
        this.log = LoggerFactory.getLogger(this.getClass());
        this.services = services;
    }

    @Override
    public OAuth2Authentication loadAuthentication(String accessToken) throws AuthenticationException, InvalidTokenException {
        for(ResourceServerTokenServices service : services){
            try{
                log.debug("Attempting to authenticate token using {}", service);
                return service.loadAuthentication(accessToken);
            }catch( AuthenticationException | InvalidTokenException | HttpClientErrorException e){
                log.debug("Failed to authenticate token with {}", service);
            }
        }
        throw new InvalidTokenException("Authentication failed. Please check your authentication token");
    }

    @Override
    public OAuth2AccessToken readAccessToken(String accessToken) {
        for(ResourceServerTokenServices service : services){
            OAuth2AccessToken token = service.readAccessToken(accessToken);
            if(token != null){
                return token;
            }
        }
        return null;
    }
}
