package com.ssafy.a705.feature.signup

object SignupNavRoutes {
    const val Signup = "signup"
    const val EmailResend = "signup_email"
    const val EmailResendWithArgs = "$EmailResend?email={email}"
    const val Success = "signup_success"
    const val EmailVerified = "email_verified?token={token}"
}
