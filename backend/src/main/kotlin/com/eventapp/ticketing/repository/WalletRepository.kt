package com.eventapp.ticketing.repository

import com.eventapp.ticketing.entity.Wallet
import com.eventapp.ticketing.entity.WalletTransaction
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository : JpaRepository<Wallet, Long> {
    fun findByUserId(userId: Long): Wallet?
}

@Repository
interface WalletTransactionRepository : JpaRepository<WalletTransaction, Long> {
    fun findByWalletIdOrderByCreatedAtDesc(walletId: Long): List<WalletTransaction>
}