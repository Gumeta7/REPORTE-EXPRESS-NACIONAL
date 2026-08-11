package com.example.data.demo

import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailEntity

object DemoData {
    val sampleProviderEmails = listOf(
        ProviderEmailEntity(providerName = "Zitro", email = "soporte@zitro.com"),
        ProviderEmailEntity(providerName = "IGT", email = "soporte@igt.com"),
        ProviderEmailEntity(providerName = "Aristocrat", email = "soporte@aristocrat.com"),
        ProviderEmailEntity(providerName = "Novomatic", email = "soporte@novomatic.com"),
        ProviderEmailEntity(providerName = "Konami", email = "soporte@konami.com"),
        ProviderEmailEntity(providerName = "Casino Interno", email = "servicio@casino.com")
    )

    val sampleMachines = listOf(
        MachineEntity(
            machineNumber = "444",
            assetNumber = "AST-0444",
            serialNumber = "SN-ZTR-8849201",
            brand = "Zitro",
            model = "Altius Glare",
            area = "Sala Principal",
            game = "Link King",
            island = "Isla Zitro 03"
        ),
        MachineEntity(
            machineNumber = "1025",
            assetNumber = "AST-1025",
            serialNumber = "SN-IGT-9921023",
            brand = "IGT",
            model = "PeakSlant49",
            area = "Zona VIP",
            game = "Wheel of Fortune",
            island = "Isla IGT 01"
        ),
        MachineEntity(
            machineNumber = "882",
            assetNumber = "AST-0882",
            serialNumber = "SN-ARI-4432109",
            brand = "Aristocrat",
            model = "MarsX Dual",
            area = "Sala Principal",
            game = "Dragon Link",
            island = "Isla Aristocrat 05"
        ),
        MachineEntity(
            machineNumber = "551",
            assetNumber = "AST-0551",
            serialNumber = "SN-NOV-6612093",
            brand = "Novomatic",
            model = "Panthera Curve",
            area = "Área B",
            game = "Cash Connection",
            island = "Isla Novomatic 02"
        ),
        MachineEntity(
            machineNumber = "302",
            assetNumber = "AST-0302",
            serialNumber = "SN-KON-1120492",
            brand = "Konami",
            model = "Dimension 49",
            area = "Zona Fumadores",
            game = "All Aboard",
            island = "Isla Konami 01"
        ),
        MachineEntity(
            machineNumber = "709",
            assetNumber = "AST-0709",
            serialNumber = "SN-BAL-7739102",
            brand = "Bally / Light & Wonder",
            model = "Kascada Wave",
            area = "Sala Principal",
            game = "Ultimate Fire Link",
            island = "Isla Bally 04"
        )
    )
}
