package com.parking.parking_manager.application.port.input

import com.parking.parking_manager.infrastructure.adapters.model.EntryEventRequest
import com.parking.parking_manager.infrastructure.adapters.model.ExitEventRequest
import com.parking.parking_manager.infrastructure.adapters.model.ParkedEventRequest

interface VehicleEventService {
    fun handleEntryEvent(event: EntryEventRequest)
    fun handleParkedEvent(event: ParkedEventRequest)
    fun handleExitEvent(event: ExitEventRequest)
}
