package uk.co.kidsloop.features.authentication

object B2CConfiguration {

    /**
     * Name of your B2C tenant hostname.
     */
    private const val azureAdB2CHostName = "login.sso.kidsloop.live"

    /**
     * Name of your B2C tenant.
     */
    private const val tenantName = "kidsloopb2c.onmicrosoft.com"

    /**
     * Returns an authority for the given policy name.
     *
     * @param policyName name of a B2C policy.
     */
    fun getAuthorityFromPolicyName(policyName: String): String {
        return "https://${azureAdB2CHostName}/${tenantName}/${policyName}/"
    }

    /**
     * Returns an array of scopes you wish to acquire as part of the returned token result.
     * These scopes must be added in your B2C application page.
     */
    val scopes: List<String>
        get() = listOf(
            "https://login.sso.kidsloop.live/010eb29e-d42b-4ca3-9c16-1961a528ce77/tasks.read",
            "https://login.sso.kidsloop.live/010eb29e-d42b-4ca3-9c16-1961a528ce77/tasks.write"
        )
}
