package com.meepleday.user

import com.meepleday.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

enum class AuthProvider { KAKAO, GOOGLE }

enum class UserRole { USER, ADMIN }

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_provider_id", columnNames = ["provider", "provider_id"])],
)
class User(

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var provider: AuthProvider,

    @Column(name = "provider_id", nullable = false)
    var providerId: String,

    /** Not every provider guarantees email (e.g. Kakao) — nullable, used for notifications only. */
    @Column
    var email: String? = null,

    @Column(name = "display_name", nullable = false)
    var displayName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

) : BaseTimeEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
