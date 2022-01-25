package com.umc.miner.src.sms;

import com.umc.miner.config.BaseException;
import com.umc.miner.config.BaseResponse;
import com.umc.miner.src.sms.model.*;
import com.umc.miner.src.user.UserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Random;

import static com.umc.miner.config.BaseResponseStatus.*;
import static com.umc.miner.utils.ValidationRegex.isRegexPhoneNum;


@RestController
@RequestMapping("/miner/sms")
public class SmsController {

    final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private final SmsService smsService;
    @Autowired
    private final UserProvider userProvider;

    public SmsController(SmsService smsService, UserProvider userProvider) {
        this.smsService = smsService;
        this.userProvider = userProvider;
    }

    /**
     * [인증번호 전송] - 서리
     * 인증문자 전송 & 해당 인증번호 저장 API
     * [POST] /miner/sms
     */
    @PostMapping("")
    @Transactional
    public BaseResponse<SmsRes> sendSms(@RequestBody Request request) {
        try {
            String recipientPhoneNum = request.getRecipientPhoneNumber();

            // 핸드폰 번호 형식 확인. 010XXXXXXXX
            if (!isRegexPhoneNum(recipientPhoneNum)) {
                return new BaseResponse<>(INVALID_PHONENUM);
            }

            // 난수 4자리 생성 -> 인증번호.
            Random rand = new Random();
            String authNum = "";
            for (int i = 0; i < 4; i++) {
                String ran = Integer.toString(rand.nextInt(10));
                authNum += ran;
            }

            // 문자 전송.
            request.setContent("[Miner] 인증번호는 " + authNum + "입니다.");
            SmsRes data = smsService.sendSms(recipientPhoneNum, request.getContent());

            try {
                // DB에 전송한 인증번호랑 문자를 받은 유저 인덱스 저장.
                PostSmsAuthReq postSmsAuthReq = new PostSmsAuthReq(recipientPhoneNum, authNum);
                smsService.postSmsAuth(postSmsAuthReq);
            } catch(BaseException exception) {
                return new BaseResponse<>((exception.getStatus()));
            }

            return new BaseResponse<>(data);
        } catch (Exception e) {
            return new BaseResponse<>(FAILED_TO_MSG);
        }
    }

}
