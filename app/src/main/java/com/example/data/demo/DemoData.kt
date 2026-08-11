package com.example.data.demo

import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailEntity

object DemoData {
    val sampleProviderEmails = listOf(
        ProviderEmailEntity(providerName = "ZITRO", email = "contactcenter@operacionesdelnorte.com"),
        ProviderEmailEntity(providerName = "AGS", email = "soporteags@playags.com"),
        ProviderEmailEntity(providerName = "EGT", email = "support-mexico@egt.com"),
        ProviderEmailEntity(providerName = "IGT", email = "lacsupport@igt.com")
    )

    val sampleMachines = emptyList<MachineEntity>()
}

