package com.parking.parking_manager.infrastructure.exception

class VehicleNotFoundException(message: String) : RuntimeException(message)
class SpotNotFoundException(message: String) : RuntimeException(message)
class SectorNotFoundException(message: String) : RuntimeException(message)
class SpotAlreadyOccupiedException(message: String) : RuntimeException(message)
class SectorFullException(message: String) : RuntimeException(message)
class ParkingSessionNotFoundException(message: String) : RuntimeException(message)
