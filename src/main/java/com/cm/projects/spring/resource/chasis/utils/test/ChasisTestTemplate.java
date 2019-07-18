package com.cm.projects.spring.resource.chasis.utils.test;

import com.cm.projects.spring.resource.chasis.wrappers.ResponseWrapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.OAuth2Request;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

/**
 * Copyright 2019 Cornelius M.
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
public class ChasisTestTemplate {
    /**
     * Test client
     */
    protected  WebTestClient webClient;
    /**
     * Token with limited access. For testing authorization
     */
     protected String GUEST_TOKEN = "221d6028-29ab-4f9d-a3cb-b6ab4659e4ff";
    /**
     * Token with all authorities. For testing access controls on resource
     */
    protected  String AUTHORIZED_TOKEN = "9cf530d8-9faa-4ff5-b95e-34822405000d";
    /**
     * Default token store
     */
    protected TokenStore tokenStore ;

    protected String endpoint = "/";

    @Before
    public void setup() {
//        this.restOperations = new OAuth2RestTemplate(this.resource("+254734788222", "Data2018."));
        //Store authorized token
        DefaultOAuth2AccessToken token = new DefaultOAuth2AccessToken(this.AUTHORIZED_TOKEN);
        OAuth2Request request = new OAuth2Request(new HashMap<>(), "test_client", List.of(), true, null, null, null, null, null);
        tokenStore.storeAccessToken(token, new OAuth2Authentication(request, new TestingAuthenticationToken("cornelius@tulaa.io", "Data2018.",
                "ROLE_PROGRAM_ADMIN", "VIEW_CONFIG", "UPDATE_CONFIG", "APPROVE_CONFIG")) );

        token = new  DefaultOAuth2AccessToken(this.GUEST_TOKEN);
        tokenStore.storeAccessToken(token, new OAuth2Authentication(request, new TestingAuthenticationToken("muhatia@tulaa.io", "Data2018")) );
    }


    /**
     * Test fetch single resources. Unit tests include:
     * <ul>
     *     <li>Incase authorization is enabled test for 401 and 403 response</li>
     *     <li>Test for empty response fon none existing resource</li>
     *     <li>Test for response 200 with data</li>
     * </ul>
     * <b>Note</b>
     * <p>Default id -1 is used to carry out tests</p>
     */
    @Test
    public void fetchSingleResource(){
        //test for access denied
        this.webClient.get().uri(this.endpoint + "/-1")
                .header("Authorization", "Bearer " +  this.GUEST_TOKEN)
                .exchange()
                .expectStatus().isForbidden();

        //test for status 200
        FluxExchangeResult<ResponseWrapper> response = this.webClient.get().uri(this.endpoint + "/-1")
                .header("Authorization", "Bearer " +  this.AUTHORIZED_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .returnResult(ResponseWrapper.class);

//        assertEquals(-1, ((ResponseWrapper<LinkedHashMap>)response.getResponseBody().blockFirst()).getData().get("id"));
    }
}
