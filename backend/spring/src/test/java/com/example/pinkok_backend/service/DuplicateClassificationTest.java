package com.example.pinkok_backend.service;

import com.example.pinkok_backend.exception.DuplicateAccountException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** 동시에 가입해서 DB 유니크 제약에 걸렸을 때, 오류 메시지로 이메일 쪽인지 아이디 쪽인지 가리는 부분. */
class DuplicateClassificationTest {

    @Test
    @DisplayName("MySQL 의 Duplicate entry 메시지에 든 값으로 이메일/아이디를 가린다")
    void classifiesByTheDuplicatedValue() {
        DuplicateAccountException byEmail = UserService.classifyDuplicate(
                "Duplicate entry 'me@pinkok.com' for key 'users.UKabc'", "me@pinkok.com", "me_user");
        assertEquals("EMAIL_DUPLICATED", byEmail.getErrorCode());

        DuplicateAccountException byUsername = UserService.classifyDuplicate(
                "Duplicate entry 'me_user' for key 'users.UKdef'", "me@pinkok.com", "me_user");
        assertEquals("USERNAME_DUPLICATED", byUsername.getErrorCode());
    }

    @Test
    @DisplayName("대소문자가 달라도 가리고, 중복 오류가 아니거나 가릴 수 없으면 null")
    void ignoresCaseAndUnrelatedErrors() {
        assertEquals("EMAIL_DUPLICATED", UserService.classifyDuplicate(
                "DUPLICATE ENTRY 'ME@PINKOK.COM' FOR KEY 'x'", "me@pinkok.com", "me_user").getErrorCode());
        assertNull(UserService.classifyDuplicate("Column 'nickname' cannot be null", "me@pinkok.com", "me_user"));
        assertNull(UserService.classifyDuplicate("Duplicate entry '77' for key 'PRIMARY'", "me@pinkok.com", "me_user"));
        assertNull(UserService.classifyDuplicate(null, "me@pinkok.com", "me_user"));
    }
}
