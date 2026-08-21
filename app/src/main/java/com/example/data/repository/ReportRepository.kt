package com.example.data.repository

import com.example.data.db.EmailReportDao
import com.example.data.db.EmailReportEntity
import com.example.data.db.MachineDao
import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailDao
import com.example.data.db.ProviderEmailEntity
import com.example.data.db.TechnicianDao
import com.example.data.db.TechnicianEntity
import com.example.data.demo.DemoData
import kotlinx.coroutines.flow.Flow

class ReportRepository(
    private val machineDao: MachineDao,
    private val emailReportDao: EmailReportDao,
    private val providerEmailDao: ProviderEmailDao,
    private val technicianDao: TechnicianDao
) {
    val allMachines: Flow<List<MachineEntity>> = machineDao.getAllMachines()
    val allReports: Flow<List<EmailReportEntity>> = emailReportDao.getAllReports()
    val allProviderEmails: Flow<List<ProviderEmailEntity>> = providerEmailDao.getAllProviderEmails()
    val distinctSalas: Flow<List<String>> = machineDao.getDistinctSalas()
    val allTechnicians: Flow<List<TechnicianEntity>> = technicianDao.getAllTechnicians()

    suspend fun checkAndInitializeDemoData() {
        val emailCount = providerEmailDao.getProviderEmailCount()
        if (emailCount == 0) {
            providerEmailDao.insertAllProviderEmails(DemoData.sampleProviderEmails)
        }
    }

    suspend fun getMachineCount(): Int {
        return machineDao.getMachineCount()
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

    suspend fun findMachineBySerialOrAssetOrQr(key: String): MachineEntity? {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return null
        return machineDao.getMachineBySerialOrAssetOrQr(trimmed)
    }

    suspend fun insertMachine(machine: MachineEntity) {
        machineDao.insertMachine(machine)
    }

    suspend fun importMachineCatalog(machines: List<MachineEntity>) {
        machineDao.insertAllMachines(machines)
    }

    suspend fun mergeAndImportMachines(incomingMachines: List<MachineEntity>) {
        val existingList = machineDao.getAllMachinesList()
        if (existingList.isEmpty()) {
            machineDao.insertAllMachines(incomingMachines)
            return
        }

        val existingByAsset = mutableMapOf<String, MachineEntity>()
        val existingBySerial = mutableMapOf<String, MachineEntity>()
        val existingByNum = mutableMapOf<String, MachineEntity>()

        for (m in existingList) {
            val a = m.assetNumber.trim().lowercase()
            val s = m.serialNumber.trim().lowercase()
            val n = m.machineNumber.trim().lowercase()
            if (a.isNotBlank()) existingByAsset[a] = m
            if (s.isNotBlank()) existingBySerial[s] = m
            if (n.isNotBlank()) existingByNum[n] = m
        }

        val mergedResults = mutableListOf<MachineEntity>()

        for (inc in incomingMachines) {
            val a = inc.assetNumber.trim().lowercase()
            val s = inc.serialNumber.trim().lowercase()
            val n = inc.machineNumber.trim().lowercase()

            val existing = (if (a.isNotBlank()) existingByAsset[a] else null)
                ?: (if (s.isNotBlank()) existingBySerial[s] else null)
                ?: (if (n.isNotBlank()) existingByNum[n] else null)

            if (existing != null) {
                // Complementamos los datos sin sobreescribir con valores vacíos
                val merged = MachineEntity(
                    id = existing.id,
                    machineNumber = inc.machineNumber.ifBlank { existing.machineNumber },
                    brand = inc.brand.ifBlank { existing.brand },
                    model = inc.model.ifBlank { existing.model },
                    serialNumber = inc.serialNumber.ifBlank { existing.serialNumber },
                    assetNumber = inc.assetNumber.ifBlank { existing.assetNumber },
                    area = inc.area.ifBlank { existing.area },
                    game = inc.game.ifBlank { existing.game },
                    island = inc.island.ifBlank { existing.island },
                    sala = inc.sala.ifBlank { existing.sala },
                    qrId = inc.qrId.ifBlank { existing.qrId },
                    propietario = inc.propietario.ifBlank { existing.propietario }
                )
                mergedResults.add(merged)
            } else {
                mergedResults.add(inc)
            }
        }

        machineDao.insertAllMachines(mergedResults)
    }

    suspend fun clearAllMachines() {
        machineDao.clearAllMachines()
    }

    suspend fun restoreDemoMachines() {
        machineDao.insertAllMachines(DemoData.sampleMachines)
    }

    // --- Technicians / Authentication ---
    suspend fun importTechnicians(technicians: List<TechnicianEntity>) {
        technicianDao.clearAllTechnicians()
        technicianDao.insertAllTechnicians(technicians)
    }

    suspend fun authenticateTechnician(user: String, pass: String): TechnicianEntity? {
        return technicianDao.authenticate(user.trim(), pass.trim())
    }

    suspend fun getTechnicianByUser(user: String): TechnicianEntity? {
        return technicianDao.getTechnicianByUser(user.trim())
    }

    suspend fun getTechnicianCount(): Int {
        return technicianDao.getTechnicianCount()
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
