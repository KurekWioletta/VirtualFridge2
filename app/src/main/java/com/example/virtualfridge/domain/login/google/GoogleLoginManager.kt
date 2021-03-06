package com.example.virtualfridge.domain.login.google

import android.content.Intent
import com.example.virtualfridge.R
import com.example.virtualfridge.data.api.UserApi
import com.example.virtualfridge.data.api.models.mapToUser
import com.example.virtualfridge.data.internal.UserDataStore
import com.example.virtualfridge.domain.base.BaseActivity
import com.example.virtualfridge.utils.ApiErrorParser
import com.example.virtualfridge.utils.RxTransformerManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject

class GoogleLoginManager @Inject constructor(
    private val activity: BaseActivity,
    private val userApi: UserApi,
    private val userDataStore: UserDataStore,
    private val apiErrorParser: ApiErrorParser,
    private val googleLoginListener: GoogleLoginListener,
    private val rxTransformerManager: RxTransformerManager
) {

    private lateinit var mGoogleSignInClient: GoogleSignInClient

    fun initializeSignInClient() {
        if (!this::mGoogleSignInClient.isInitialized) {
            mGoogleSignInClient = GoogleSignIn.getClient(
                activity,
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    // TODO: think of authorization and authentication
                    //.requestServerAuthCode(serverClientId)
                    .requestEmail()
                    .build()
            )
        }
    }

    fun userLoggedIn() = GoogleSignIn.getLastSignedInAccount(activity) != null

    fun requestLogin() =
        googleLoginListener.openGoogleLoginRequest(mGoogleSignInClient.signInIntent)

    fun login(intent: Intent?) {
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(intent)
                .getResult(ApiException::class.java)
            if (account != null) {
                FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        // TODO: error tracking
                        activity.showAlert(activity.getString(R.string.login_login_error))
                        return@OnCompleteListener
                    }

                    activity.registerViewSubscription(userApi
                        .registerUserWithGoogle(
                            account.email ?: "",
                            account.id ?: "",
                            account.givenName ?: "",
                            account.familyName ?: ""
                        )
                        .doOnNext { userDataStore.cacheUser(it.mapToUser()) }
                        .flatMap {
                            userApi.notifications(
                                userId = it.id,
                                messagingToken = task.result
                            )
                        }
                        .compose { rxTransformerManager.applyIOScheduler(it) }
                        .doOnSubscribe { activity.showLoading() }
                        .doOnTerminate { activity.hideLoading() }
                        .doOnError { logout() }
                        .subscribe({
                            googleLoginListener.openMainActivity()
                        }, {
                            activity.showAlert(apiErrorParser.parse(it))
                        })
                    )
                }).addOnFailureListener {
                    googleLoginListener.showGoogleLoginError()
                }
            }
        } catch (e: ApiException) {
            googleLoginListener.showGoogleLoginError()
        }
    }

    fun logout() = mGoogleSignInClient.signOut()
        .addOnCompleteListener(activity) {
        }

    fun deleteAccount() = mGoogleSignInClient.revokeAccess()
        .addOnCompleteListener(activity) {
        }

}