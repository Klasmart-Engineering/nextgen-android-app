package uk.co.kidsloop.app.utils

fun String.getInitials(): String {
    var initials = ""
    for (s in this.split(" ")) {
        initials += s[0]
    }
    return initials
}
