package elemsocial.com.config

object AppConfig {
    object Session {
        const val DEVICE_NAME = "Element Android 1.0"
        const val DEVICE_TYPE = "android_app"
    }

    object Domains {
        val DEFAULT_USER_WS_URLS = listOf(
            "wss://ws.elemsocial.com/user_api"
        )
        const val DEFAULT_USER_WS_URL = "wss://ws.elemsocial.com/user_api"
    }

    object Push {
        const val VAPID_PUBLIC_KEY = "BP2xfmqDnX7-yoDsZQxgHt8aTd7fSRhLno0-fPwpGoglILifPqzVmEo0OLNYILeU0qVkC5qo_rLhzzcrBh_EIIs"
    }

}
