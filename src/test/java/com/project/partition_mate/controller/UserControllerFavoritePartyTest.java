package com.project.partition_mate.controller;

import com.project.partition_mate.domain.PartyStatus;
import com.project.partition_mate.domain.User;
import com.project.partition_mate.dto.PartyResponse;
import com.project.partition_mate.security.CustomUserDetails;
import com.project.partition_mate.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerFavoritePartyTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private User currentUser;

    @BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new com.project.partition_mate.exception.GlobalExceptionHandler())
                .build();
        this.currentUser = new User("tester", "tester@test.com", "pw", "서울", 37.5, 127.0);
        ReflectionTestUtils.setField(currentUser, "id", 1L);

        CustomUserDetails principal = new CustomUserDetails(currentUser);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 관심_파티_목록을_조회한다() throws Exception {
        // given
        given(userService.getFavoriteParties(currentUser))
                .willReturn(List.of(createFavoritePartyResponse(7L, "양재점 연어 소분")));

        // when
        // then
        mockMvc.perform(get("/api/users/me/favorite-parties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].title").value("양재점 연어 소분"))
                .andExpect(jsonPath("$[0].favorite").value(true));
    }

    @Test
    void 관심_파티를_저장한다() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(put("/api/users/me/favorite-parties/9"))
                .andExpect(status().isNoContent());

        then(userService).should().saveFavoriteParty(currentUser, 9L);
    }

    @Test
    void 관심_파티를_해제한다() throws Exception {
        // given

        // when
        // then
        mockMvc.perform(delete("/api/users/me/favorite-parties/9"))
                .andExpect(status().isNoContent());

        then(userService).should().removeFavoriteParty(currentUser, 9L);
    }

    private PartyResponse createFavoritePartyResponse(Long partyId, String title) {
        return new PartyResponse(
                partyId,
                title,
                "상품명",
                19900,
                4,
                PartyStatus.RECRUITING,
                "코스트코 양재점",
                2,
                "https://open.kakao.com/o/favorite"
        ).withFavorite(true);
    }
}
