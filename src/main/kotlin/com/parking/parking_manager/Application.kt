package com.parking.parking_manager

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ParkingManagerApplication

fun main(args: Array<String>) {
	runApplication<ParkingManagerApplication>(*args)
}
