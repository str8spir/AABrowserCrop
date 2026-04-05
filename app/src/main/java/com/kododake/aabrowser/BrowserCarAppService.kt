package com.kododake.aabrowser

import android.content.Intent
import androidx.car.app.CarAppService
import androidx.car.app.Session
import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.MessageTemplate
import androidx.car.app.model.Template
import androidx.car.app.validation.HostValidator

class BrowserCarAppService : CarAppService() {
    override fun createHostValidator(): HostValidator {
        return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR
    }

    override fun onCreateSession(): Session {
        return object : Session() {
            override fun onCreateScreen(intent: Intent): Screen {
                return object : Screen(carContext) {
                    override fun onGetTemplate(): Template {
                        return MessageTemplate.Builder("AA Browser is active. Please use the car's native app launcher.")
                            .setTitle("AA Browser")
                            .setHeaderAction(Action.APP_ICON)
                            .build()
                    }
                }
            }
        }
    }
}
