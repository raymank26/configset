package com.letsconfig.users

import org.skife.jdbi.v2.DBI

/**
 * @author anton.ermak
 *         Date: 15.02.17.
 */
class UsersDAO(private val dbi: DBI) {

    fun createUser(user: User) {
        val handle = dbi.open()
        handle.execute("insert into users (email, activated", user.email, user.activated)
    }

}
