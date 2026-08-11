package com.example.data.repository

import com.example.data.db.EmailReportDao
import com.example.data.db.EmailReportEntity
import com.example.data.db.MachineDao
import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailDao
import com.example.data.db.ProviderEmailEntity
import com.example.data.demo.DemoData
import kotlinx.coroutines.flow.Flow

class ReportRepository(
    private val machineDao: MachineDao,
    private val emailReportDao: EmailReportDao,
    private val providerEmailDao: ProviderEmailDao
) {
    val allMachines: Flow<List<MachineEntity>> = machineDao.getAllMachines()
    val allReports: Flow<List<EmailReportEntity>> = emailReportDao.getAllReports()
    val allProviderEmails: Flow<List<ProviderEmailEntity>> = providerEmailDao.getAllProviderEmails()

    suspend fun checkAndInitializeDemoData() {
        val emailCount = providerEmailDao.getProviderEmailCount()
        if (emailCount == 0) {
            providerEmailDao.insertAllProviderEmails(DemoData.sampleProviderEmails)
        }
    }

    fun searchMachines(query: String): Flow<List<MachineEntity>> {
        return if (query.isBlank()) {
            machineDao.getAllMachines()
        } else {
            machineDao.searchMachines(query)
        }
    }

    suspend fun findMachine(query: String): MachineEntity? {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return null
        return machineDao.getMachineByNumber(trimmed)
    }

    suspend fun insertMachine(machine: MachineEntity) {
        machineDao.insertMachine(machine)
    }

    suspend fun importMachineCatalog(machines: List<MachineEntity>) {
        machineDao.insertAllMachines(machines)
    }

    suspend fun clearAllMachines() {
        machineDao.clearAllMachines()
    }

    suspend fun restoreDemoMachines() {
        machineDao.insertAllMachines(DemoData.sampleMachines)
    }

    // --- Provider Emails ---
    suspend fun insertProviderEmail(providerEmail: ProviderEmailEntity) {
        providerEmailDao.insertProviderEmail(providerEmail)
    }

    suspend fun deleteProviderEmail(id: Int) {
        providerEmailDao.deleteProviderEmailById(id)
    }

    suspend fun clearAllProviderEmails() {
        providerEmailDao.clearAllProviderEmails()
    }

    suspend fun restoreDemoProviders() {
        providerEmailDao.insertAllProviderEmails(DemoData.sampleProviderEmails)
    }

    // --- Reports ---
    fun searchReports(query: String): Flow<List<EmailReportEntity>> {
        return if (query.isBlank()) {
            emailReportDao.getAllReports()
        } else {
            emailReportDao.searchReports(query)
        }
    }

    suspend fun saveReport(report: EmailReportEntity): Long {
        return emailReportDao.insertReport(report)
    }

    suspend fun deleteReport(id: Int) {
        emailReportDao.deleteReportById(id)
    }

    suspend fun clearAllReports() {
        emailReportDao.clearAllReports()
    }
}
