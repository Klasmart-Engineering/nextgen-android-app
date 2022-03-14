package uk.co.kidsloop.features.authentication

object B2CConfiguration {

    /**
     * Name of your B2C tenant hostname.
     */
    private const val azureAdB2CHostName = "login.alpha.kidsloop.net"

    /**
     * Name of your B2C tenant.
     */
    private const val tenantName = "klkralpha.onmicrosoft.com"

    /**
     * Returns an authority for the given policy name.
     *
     * @param policyName name of a B2C policy.
     */
    fun getAuthorityFromPolicyName(policyName: String): String {
        return "https://$azureAdB2CHostName/$tenantName/$policyName/"
    }

    /**
     * Returns an array of scopes you wish to acquire as part of the returned token result.
     * These scopes must be added in your B2C application page.
     */
    val scopes: List<String>
        get() = listOf(
            "https://login.alpha.kidsloop.net/63a170f9-9d0c-4198-b587-8c63ab59ebdf/tasks.write"
        )
}
