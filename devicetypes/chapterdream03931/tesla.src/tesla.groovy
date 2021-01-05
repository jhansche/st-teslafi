/**
 *  Tesla
 *
 *  Copyright 2019 Joe Hansche
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition(
            name: "Tesla",
            namespace: "chapterdream03931",
            author: "Joe Hansche",
            mnmn: "SmartThingsCommunity",
            ocfDeviceType: "x.com.st.d.tesla",
            vid: "d45508a6-ff8a-345c-8898-61cb0b3a9466"
    ) {
        // .vin, .hwVersion, .swVersion, .odometerMiles,
        // .carState = [Sleeping | Idling | Driving]
        // FIXME: do we also need .ev.carState = [Charging, Sentry]?
        //  otherwise maybe just ev.chargingState is enough? and move Sentry=Alarm?
        // wake, refresh
        capability "chapterdream03931.automobile"

        // .batteryRange, .chargingState
        // chargeStart, chargeStop, chargePortOpen, chargePortClose
        capability "chapterdream03931.electricVehicle"


        capability "Battery" // .battery
        capability "Energy Meter" // .energy = $ kWh
        capability "Geolocation" // lat/long, speed, heading
        capability "Lock" // .locked
        capability "Motion Sensor" // .motion = active | inactive
        // capability "Power Consumption Report" // https://docs.smartthings.com/en/latest/capabilities-reference.html#id63
        capability "Power Meter" // .power = $ W
        capability "Power Source" // .powerSource = [battery | dc | mains | unknown]
        capability "Presence Sensor" // .presence = [present, not present]
        capability "Refresh" // refresh()
        capability "Sleep Sensor" // .sleeping = [sleeping, not_sleeping]
        capability "Temperature Measurement"
        capability "Thermostat Mode" // .thermostatMode = [auto, off]
        capability "Thermostat Setpoint"
        capability "Timed Session" // .completionTime, .sessionStatus; https://docs.smartthings.com/en/latest/capabilities-reference.html#id97
        capability "Tone" // use it for the horn?
        capability "Voltage Measurement" // .voltage = $ V

        // FIXME: move these to capability
        attribute "onlineState", "string"

        // FIXME: Why doesn't Thermostat Setpoint define this?
        command "setThermostatSetpoint"
    }
}

def initialize() {
    log.debug "Executing 'initialize'"

    sendEvent(name: "supportedThermostatModes", value: ["auto", "off"])

    runIn(2, doRefresh)
    runEvery5Minutes(doRefresh)
}

private processData(data) {
    log.debug "processData: ${data}"
    if (!data) {
        log.error "No data found for ${device.deviceNetworkId}"
        return
    }

    if (device.latestValue("vin") != data.vin) {
        // How?
        sendEvent(name: "chapterdream03931.automobile.vin", value: data.vin)
    }
    if (device.latestValue("swVersion") != data.version) {
        log.info "New Software version: ${data.version}"
        sendEvent(name: "chapterdream03931.automobile.swVersion", value: data.version)
    }

    sendEvent(name: "onlineState", value: data.state)

    if (device.latestValue("carState") != data.car_state) {
        sendEvent(name: "chapterdream03931.automobile.carState", value: data.car_state)
    }

    sendEvent(name: "sleeping", value: data.sleep_state)
    sendEvent(name: "motion", value: data.motion)

    if (data.chargeState) {
        // Battery
        sendEvent(name: "battery", value: data.chargeState.battery, unit: '%')
        
        // FIXME: none of these are working correctly
        sendEvent(name: "chargeMax", value: data.chargeState.chargeMax, unit: '%')
        // XXX: wtf is happening here?
        // error java.lang.NullPointerException: Cannot get property 'value' on null object @line 102 (processData)
//		sendEvent(name: "chapterdream03931.electricVehicle.chargeMax", value: 36.toFloat())

        if (device.currentValue("batteryRange")?.toFloat() != data.chargeState.batteryRange) {
            sendEvent(name: "chapterdream03931.electricVehicle.batteryRange", value: data.chargeState.batteryRange, unit: 'mi')
        }

        if (device.currentValue("chargingState") != data.chargeState.chargingState) {
            sendEvent(name: "chapterdream03931.electricVehicle.chargingState", value: data.chargeState.chargingState)
        }

        // battery, dc, mains, unknown
        // TODO: when supercharging, powerSource=dc
        if (data.chargeState.chargingState == "not_charging") {
            sendEvent(name: "powerSource", value: "battery")
        } else if (data.chargeState.fastChargerPresent) {
            // Assuming that fastChargerPresent => Supercharger => DC
            sendEvent(name: "powerSource", value: "dc")
        } else {
            sendEvent(name: "powerSource", value: "mains")
        }

        sendEvent(name: "energy", value: data.chargeState.chargeEnergyAdded, unit: 'kWh')
        sendEvent(name: "voltage", value: data.chargeState.chargerVoltage, unit: 'V')
        sendEvent(name: "power", value: data.chargeState.chargerPower, unit: 'kW') // kW to W

        if (data.chargeState.chargingState == "charging") {
            sendEvent(name: "chargeTimeRemaining", value: data.chargeState.hoursRemaining, unit: 'h')

            // Timed Session
            if (data.chargeState.hoursRemaining != null) {
                def minutesRemaining = (data.chargeState.hoursRemaining as float) * 60
                def eta = Calendar.getInstance(location.timeZone)
                eta.set(Calendar.SECOND, 0)
                eta.set(Calendar.MILLISECOND, 0)
                eta.add(Calendar.MINUTE, Math.round(minutesRemaining as float))

                if (minutesRemaining > 120) {
                    // If it's going to be longer than 2 hours, just round completion time to the nearest quarter-hour
                    def delta = eta.get(Calendar.MINUTE) % 15
                    eta.add(Calendar.MINUTE, delta < 8 ? -delta : (15 - delta))
                }

                sendEvent(name: "completionTime", value: eta.time.format("EEE MMM dd HH:mm:ss zzz yyyy", location.timeZone))
            }
            sendEvent(name: "sessionStatus", value: "running")
        } else {
            // clear it if it was set
            sendEvent(name: "chargeTimeRemaining", value: null)
            sendEvent(name: "sessionStatus", value: "stopped")
        }
    }

    if (data.driveState) {
        // Geolocation:
        if (data.driveState.speed < 0) {
            // FIXME this should be in the presentation
            sendEvent(name: "speed", value: "parked", isStateChange: false)
        } else {
            sendEvent(name: "speed", value: data.driveState.speed, unit: "mph")
        }
        sendEvent(name: "latitude", value: data.driveState.latitude)
        sendEvent(name: "longitude", value: data.driveState.longitude)
        sendEvent(name: "method", value: data.driveState.method)
        sendEvent(name: "heading", value: data.driveState.heading)
        // lastUpdateTime = gps_as_of (unix timestamp)
        sendEvent(name: "lastUpdateTime", value: data.driveState.lastUpdateTime, displayed: false)
    }

    if (data.vehicleState) {
        sendEvent(name: "presence", value: data.vehicleState.presence)
        sendEvent(name: "lock", value: data.vehicleState.lock)
        sendEvent(name: "chapterdream03931.automobile.odometerMiles", value: data.vehicleState.odometer, unit: 'mi')
    }

    if (data.climateState) {
        sendEvent(name: "temperature", value: data.climateState.temperature, unit: 'F')
        sendEvent(name: "thermostatSetpoint", value: data.climateState.thermostatSetpoint, unit: 'F')
        sendEvent(name: "thermostatMode", value: data.climateState.thermostatMode)
        // TODO: needs another child device? Already using Temperature Measurement for inside temp...
        sendEvent(name: "temperatureOutside", value: data.climateState.outsideTemp, unit: 'F')
    }
}

def doRefresh() {
    log.debug "Refreshing car data now; last update=${device.getLastActivity()}"
    def data = parent.refresh(this)
    processData(data)

    if (data?.car_state == 'Driving') {
        log.debug "Refreshing more often because Driving"
        runEvery1Minute(refreshWhileDriving)
    }
}

// Only use this for the manual refresh tile action
def refresh() {
    log.debug "Triggering refresh by command"
    doRefresh()
}

def refreshWhileDriving() {
    log.debug "Executing 'refreshWhileDriving'"
    def data = parent.refresh(this)
    processData(data)

    if (data?.car_state != "Driving") {
        unschedule(refreshWhileDriving)
    }
}

def beep() {
    log.debug "Executing 'beep'"
    def data = parent.beep(this)
}

def wake() {
    log.debug "Executing 'wake'"
    def result = parent.wake(this)
    if (result) doRefresh()
}

def lock() {
    log.debug "Executing 'lock'"
    def result = parent.lock(this)
    if (result) doRefresh()
}

def unlock() {
    log.debug "Executing 'unlock'"
    def result = parent.unlock(this)
    if (result) doRefresh()
}

def auto() {
    log.debug "Executing 'thermostatMode.auto'"
    def result = parent.climateAuto(this)
    if (result) doRefresh()
}

def off() {
    log.debug "Executing 'thermostatMode.off'"
    def result = parent.climateOff(this)
    if (result) doRefresh()
}

def heat() { log.info "Executing 'thermostatMode.heat' - Not supported" }

def emergencyHeat() { log.info "Executing 'thermostatMode.emergencyHeat' - Not supported" }

def cool() { log.info "Executing 'thermostatMode.cool' - Not supported" }

def setThermostatMode(mode) {
    log.debug "Executing 'setThermostatMode'"
    if (mode == "auto") {
        auto()
    } else if (mode == "off") {
        off()
    } else {
        log.error "setThermostatMode: Only thermostat modes Auto and Off are supported"
    }
}

// FIXME: Thermostat.thermostatSetpoint is deprecated, replaced with heatingSetpoint / coolingSetpoint.
//  thermostatSetpoint capability does not have a setThermostatSetpoint defined.
def setThermostatSetpoint(setpoint) {
    log.debug "Executing 'thermostat.setThermostatSetpoint'"
    def result = parent.setThermostatSetpoint(this, setpoint)
    if (result) doRefresh()
}

def chargeStart() {
    log.debug "Executing 'startCharge'"
    def result = parent.startCharge(this)
    if (result) doRefresh()
}

def chargeStop() {
    log.debug "Executing 'stopCharge'"
    def result = parent.stopCharge(this)
    if (result) doRefresh()
}
