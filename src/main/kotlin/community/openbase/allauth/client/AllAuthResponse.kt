package community.openbase.allauth.client

public data class AllAuthFieldError(
    val param: String?,
    val message: String,
)

public data class AllAuthResponse(
    val httpStatusCode: Int,
    val body: Map<String, Any?>,
) {
    public val status: Int get() = intAt("status") ?: httpStatusCode
    public val isSuccess: Boolean get() = status == 200
    public val requiresAuth: Boolean get() = status == 401
    public val requiresReauthentication: Boolean
        get() = status == 401 && flows.any {
            it["id"] == AuthFlow.REAUTHENTICATE.id || it["id"] == AuthFlow.MFA_REAUTHENTICATE.id
        }
    public val isAuthenticated: Boolean get() = booleanAt("meta", "is_authenticated") ?: false
    public val user: Map<String, Any?>? get() = mapAt("data", "user")

    public val flows: List<Map<String, Any?>>
        get() = listAt("data", "flows").mapNotNull { it as? Map<String, Any?> }

    public val pendingFlows: List<Map<String, Any?>>
        get() = flows.filter { it["is_pending"] as? Boolean == true }

    public val hasPendingFlows: Boolean get() = pendingFlows.isNotEmpty()

    public val errors: List<AllAuthFieldError>
        get() = listAt("errors").mapNotNull { item ->
            val map = item as? Map<*, *> ?: return@mapNotNull null
            AllAuthFieldError(
                param = map["param"] as? String,
                message = map["message"] as? String ?: return@mapNotNull null,
            )
        }

    public val generalErrors: List<String>
        get() = errors.filter { it.param == null }.map { it.message }

    public fun pendingFlow(flow: AuthFlow): Map<String, Any?>? =
        pendingFlows.firstOrNull { it["id"] == flow.id }

    public fun error(field: String): String? =
        errors.firstOrNull { it.param == field }?.message

    public fun get(vararg path: String): Any? =
        body.valueAt(path.toList())

    public fun stringAt(vararg path: String): String? =
        get(*path) as? String

    public fun booleanAt(vararg path: String): Boolean? =
        get(*path) as? Boolean

    public fun intAt(vararg path: String): Int? =
        when (val value = get(*path)) {
            is Int -> value
            is Long -> value.toInt()
            is Double -> value.toInt()
            is Float -> value.toInt()
            is Number -> value.toInt()
            else -> null
        }

    public fun mapAt(vararg path: String): Map<String, Any?>? =
        get(*path) as? Map<String, Any?>

    public fun listAt(vararg path: String): List<Any?> =
        get(*path) as? List<Any?> ?: emptyList()
}

internal fun Map<String, Any?>.valueAt(path: List<String>): Any? {
    var cursor: Any? = this
    for (part in path) {
        cursor = (cursor as? Map<*, *>)?.get(part) ?: return null
    }
    return cursor
}

