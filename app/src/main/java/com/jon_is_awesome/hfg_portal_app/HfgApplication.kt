package com.jon_is_awesome.hfg_portal_app

import android.app.Application
import com.jon_is_awesome.hfg_portal_app.data.local.AppDatabase
import com.jon_is_awesome.hfg_portal_app.data.repository.AppRepository

class HfgApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { AppRepository(database.appDao()) }
}
