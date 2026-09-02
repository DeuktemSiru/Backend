package com.deuktemsiru.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(prefix = "app.fcm", name = ["enabled"], havingValue = "true")
class FirebaseConfig {

    // ponytail: 자격증명은 ADC 표준(GOOGLE_APPLICATION_CREDENTIALS 환경변수)만 사용한다.
    // 다른 경로를 쓰려면 그 환경변수를 바꾼다. 별도 프로퍼티가 필요해지면 그때 추가.
    @Bean
    fun firebaseMessaging(): FirebaseMessaging = FirebaseMessaging.getInstance(
        FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(
            FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build(),
        ),
    )
}
