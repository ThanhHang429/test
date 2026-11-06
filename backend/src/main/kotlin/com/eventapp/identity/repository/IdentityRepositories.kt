package com.eventapp.identity.repository

import com.eventapp.common.security.UserRoleProvider
import com.eventapp.identity.entity.OrganizerProfile
import com.eventapp.identity.entity.Role
import com.eventapp.identity.entity.User
import com.eventapp.identity.entity.UserRole
import com.eventapp.identity.entity.UserRoleId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Component
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmailIgnoreCase(email: String): User?
    fun findByPhone(phone: String): User?
}

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByRoleCode(roleCode: String): Role?

    @Query(
        """
        select r.roleCode from Role r, UserRole ur
        where ur.id.roleId = r.roleId and ur.id.userId = :userId
        """
    )
    fun findRoleCodesByUserId(userId: Long): List<String>
}

@Repository
interface UserRoleRepository : JpaRepository<UserRole, UserRoleId>

@Repository
interface OrganizerProfileRepository : JpaRepository<OrganizerProfile, Long> {
    fun findByUserId(userId: Long): OrganizerProfile?
}

@Component
class UserRoleProviderImpl(
    private val roleRepository: RoleRepository,
    private val userRepository: UserRepository
) : UserRoleProvider {
    override fun rolesOf(userId: Long): List<String> = roleRepository.findRoleCodesByUserId(userId)

    override fun exists(userId: Long): Boolean = userRepository.existsById(userId)
}
