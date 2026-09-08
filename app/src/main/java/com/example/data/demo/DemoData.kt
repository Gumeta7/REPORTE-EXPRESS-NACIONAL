package com.example.data.demo

import com.example.data.db.MachineEntity
import com.example.data.db.ProviderEmailEntity

object DemoData {
    val sampleProviderEmails = listOf(
        ProviderEmailEntity(providerName = "WINPOT", email = "atorres@winpot.com.mx", ccEmails = "aparra@winpot.com.mx, mrivera@winpot.com.mx, fcruz@winpot.com.mx"),
        ProviderEmailEntity(providerName = "ZITRO", email = "contactcenter@operacionesdelnorte.com", ccEmails = "guillermol@operacionesdelnorte.com, atorres@winpot.com.mx, aparra@winpot.com.mx, mrivera@winpot.com.mx, fcruz@winpot.com.mx"),
        ProviderEmailEntity(providerName = "DREIDEL", email = "helpdesk@dreidel.mx", ccEmails = "slevy@dreidel.mx, atorres@winpot.com.mx, aparra@winpot.com.mx, mrivera@winpot.com.mx, fcruz@winpot.com.mx"),
        ProviderEmailEntity(providerName = "CADILLAC JACK", email = "soporteags@playags.com", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx, mrivera@winpot.com.mx, fcruz@winpot.com.mx"),
        ProviderEmailEntity(providerName = "CADILLAC", email = "soporteags@playags.com", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx, mrivera@winpot.com.mx, fcruz@winpot.com.mx"),
        ProviderEmailEntity(providerName = "AGS", email = "soporteags@playags.com", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx, mrivera@winpot.com.mx, fcruz@winpot.com.mx"),
        ProviderEmailEntity(providerName = "EGT", email = "support-mexico@egt.com", ccEmails = "juan.montoya@egt.com, alex.pena@egt.com"),
        ProviderEmailEntity(providerName = "IGT", email = "lacsupport@igt.com", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx"),
        ProviderEmailEntity(providerName = "AURIFY", email = "esandoval@aurifygaming.com", ccEmails = "cmiranda@aurifygaming.com, elgonzalez@aurifygaming.com"),
        ProviderEmailEntity(providerName = "AINSWORTH", email = "CallCenterMX@agtslots.com", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx"),
        ProviderEmailEntity(providerName = "BALLY", email = "atorres@winpot.com.mx", ccEmails = "aparra@winpot.com.mx, mrivera@winpot.com.mx"),
        ProviderEmailEntity(providerName = "BETSTONE", email = "betstonemexicosupport@betstone.com", ccEmails = "Helpdesk@betstone.com, carlos.lopez@betstone.com"),
        ProviderEmailEntity(providerName = "EGAMING", email = "agustin.ruiz@egaming.mx", ccEmails = "carlos.orozco@egaming.mx, martin.garcia@egaming.mx"),
        ProviderEmailEntity(providerName = "FBM", email = "callcentermx@agtslots.com.mx", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx"),
        ProviderEmailEntity(providerName = "ALFASTREET", email = "mailcenter@pyramidgames.com.mx", ccEmails = "atorres@winpot.com.mx, aparra@winpot.com.mx"),
        ProviderEmailEntity(providerName = "KONAMI", email = "soportekgi@gmail.com", ccEmails = "carrion0124@konamigaming.com, atorres@winpot.com.mx")
    )

    val sampleMachines = emptyList<MachineEntity>()
}

