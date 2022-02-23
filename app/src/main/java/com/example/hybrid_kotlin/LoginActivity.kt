package com.dealer.allcar

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.kakao.auth.Session
import com.kakao.sdk.auth.model.AccessTokenResponse
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.util.Utility
import com.kakao.sdk.user.UserApiClient
import com.kakao.sdk.user.model.User
import com.kakao.sdk.user.model.UserShippingAddresses
import java.util.ArrayList

class LoginActivity : AppCompatActivity() {

    private val TAG = "LoginActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        var keyHash = Utility.getKeyHash(this)
//        Log.e("Hash",keyHash) // VlyYhq3gyf1M/3su7IXSoOAKzok=

        var kakaoId = "";
        var nickname = "";
        var email = "";
        var gender = "";
        var address = "";
        var name = "";
        var account = "";
        var ageRange = "";
        var birthday = "";
        var phoneNumber = "";
;
        // 로그인 조합 예제
        // 로그인 공통 callback 구성
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "로그인 실패", error)
            } else if (token != null) {
                Log.e(TAG, "로그인 성공 ${token.accessToken}") // 콜백 전달받음


                val keys: List<String> = ArrayList()

                keys.plus("kakao_account.profile")
                keys.plus("kakao_account.email")

                UserApiClient.instance.shippingAddresses  { userShippingAddresses, error ->
                    if (error != null) {
                        Log.e(TAG, "배송지 조회 실패", error)
                    } else if (userShippingAddresses != null) {
                        if (userShippingAddresses.shippingAddresses != null) {
                            Log.e(
                                TAG, "배송지 조회 성공" +
                                        "\n회원번호: ${userShippingAddresses.userId}" +
                                        "\n배송지: \n${userShippingAddresses.shippingAddresses?.joinToString("\n")}" +
                                        "\n이름: ${userShippingAddresses?.shippingAddresses?.component1()?.name} "

                            )
                            address = "${userShippingAddresses.shippingAddresses?.joinToString("\n")}"
                            //kakaoId = "${userShippingAddresses.userId}"
                            name = "${userShippingAddresses?.shippingAddresses?.component1()?.name}"

                            intent.putExtra("kkId", kakaoId)
                            intent.putExtra("address", address)
                            intent.putExtra("name", name)
                            setResult(RESULT_OK, intent)

                        }
                    }
                }

                UserApiClient.instance.me { user, error ->
                    if (error != null) {
                        Log.e(TAG, "사용자 정보 요청 실패", error)
                    } else if (user != null) {
                        Log.e(
                            TAG, "사용자 정보 요청 성공" +
                                    "\n회원번호: ${user.id}" +
                                    "\n이메일: ${user.kakaoAccount?.email}" +
                                    "\n닉네임: ${user.kakaoAccount?.profile?.nickname}" +
                                    "\n프로필사진: ${user.kakaoAccount?.profile?.thumbnailImageUrl}" +
                                    "\n 프로필 : ${user.kakaoAccount?.profile}" +
                                    "\n 어카운트 : ${user.kakaoAccount} " +
                                    "\n 연령대 : ${user.kakaoAccount?.ageRange}" +
                                    "\n 성별 : ${user.kakaoAccount?.gender}" +
                                    "\n 생일 : ${user.kakaoAccount?.birthday}"
                        )
                        // 추가정보 : 주소지(shippingAddresses) , 성별


                        kakaoId = "${user.id}"
                        email = "${user.kakaoAccount?.email}"
                        nickname = "${user.kakaoAccount?.profile?.nickname}"
                        gender = "${user.kakaoAccount?.gender}"
//                        account = "${user.kakaoAccount}"
                        account = "app"
                        Log.e("account",account)
                        ageRange = "${user.kakaoAccount?.ageRange}"
                        birthday = "${user.kakaoAccount?.birthday}"
                        phoneNumber = "${user.kakaoAccount?.phoneNumber}"
                        Log.e("phone",phoneNumber)

                        intent.putExtra("kkId", kakaoId)
                        intent.putExtra("nickname", nickname)
                        intent.putExtra("email", email)
                        intent.putExtra("gender", gender)
                        intent.putExtra("ageRange", ageRange)
                        intent.putExtra("birthday",birthday)
                        intent.putExtra("phoneNumber",phoneNumber)


                        // 인텐트에 전달된 데이터 셋팅
                        setResult(RESULT_OK, intent)

                        // 현재 액티비티 종료(백버튼과 동일한효과) -> 화면넘기는 역할
                        finish()

                        // 추가로 넘겨야할 정보 : 폰번호,주소지
                        //finishWithSuccess(kakaoId, nickname, email)

                    }

                }


            }


        }


        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this@LoginActivity)) {
            UserApiClient.instance.loginWithKakaoTalk(
                this@LoginActivity,
                callback = callback
            ) // 먼저 실행
            Log.e("카카오 로그인", "성공")


        } else {
            UserApiClient.instance.loginWithKakaoAccount(this@LoginActivity, callback = callback)
            Log.e("카카오 계정 로그인", "성공")

        }

    }

    /*
        :: TODO :: 로그아웃, UNLINK 테스트 해야됨
    */
    private fun onClickUnlink() {
        val appendMessage = getString(R.string.com_kakao_confirm_unlink)
        AlertDialog.Builder(this)
            .setMessage(appendMessage)
            .setPositiveButton(
                getString(R.string.com_kakao_ok_button)
            ) { dialog, which ->
                UserApiClient.instance.unlink { error ->
                    if (error != null) {
                        Log.e(TAG, "연결 끊기 실패", error)
                    }
                    else {
                        Log.i(TAG, "연결 끊기 성공. SDK에서 토큰 삭제 됨")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(
                getString(R.string.com_kakao_cancel_button)
            ) { dialog, which -> dialog.dismiss() }.show()
    }


    /* 역할? -> 데이터 전달 이 함수의 목적은 호출된 Activity가 상태에 대한 힌트를 보내도록 하여 이 기본 Activity가 노출될 준비를 할 수 있도록 하는 것입니다.
     메서드에 대한 호출은 호출된 Activity가 곧 종료되거나 종료될 것임을 보장하지 않습니다. 이 활동의 창을 노출하고 이를 준비하기 위해 전달할 일부 데이터가 있음을 나타냅니다. */
    override fun onActivityReenter(resultCode: Int, data: Intent?) {
        super.onActivityReenter(resultCode, data)
    }





}

