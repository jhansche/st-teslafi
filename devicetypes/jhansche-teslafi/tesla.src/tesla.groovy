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
    definition(name: "Tesla", namespace: "jhansche.teslafi", author: "Joe Hansche") {
        capability "Actuator"
        capability "Battery"
        capability "Energy Meter" // .energy = $ kWh
        capability "Geolocation"
        capability "Lock"
        capability "Motion Sensor"
        // capability "Power Consumption Report" // https://docs.smartthings.com/en/latest/capabilities-reference.html#id63
        capability "Power Meter" // .power = $ W
        capability "Power Source" // .powerSource = [battery | dc | mains | unknown]
        capability "Presence Sensor"
        capability "Refresh"
        capability "Sleep Sensor"
        capability "Temperature Measurement"
        capability "Thermostat Mode"
        capability "Thermostat Setpoint"
        capability "Timed Session" // .completionTime, .sessionStatus; https://docs.smartthings.com/en/latest/capabilities-reference.html#id97
        capability "Tone"
        capability "Voltage Measurement" // .voltage = $ V

        attribute "carState", "string"
        attribute "onlineState", "string"
        attribute "marqueeText", "string"
        attribute "vin", "string"
        attribute "version", "string"
        attribute "odometer", "number"
        attribute "batteryRange", "number"
        attribute "chargingState", "string"

        command "wake"
        command "setThermostatSetpoint"
        command "startCharge"
        command "stopCharge"
    }

    simulator {
        // TODO: define status and reply messages here
    }

    tiles(scale: 2) {
        multiAttributeTile(name: "multi", type: "generic", width: 6, height: 4) {
            tileAttribute("device.carState", key: "PRIMARY_CONTROL") {
                attributeState "Idling", label: "Parked", backgroundColor: "#888888"
                attributeState "Charging", label: "Charging", backgroundColor: "#44b621"
                attributeState "Driving", label: "Driving", backgroundColor: "#bc2323"
                attributeState "Sleeping", label: "Sleeping", backgroundColor: "#cccccc"
                attributeState "Unknown", label: "???", defaultState: true
            }

            // FIXME: MARQUEE not supported in generic type
            /*
            //  "Charging: ${timeToFull} hours remaining"
            //  "Parked: ${location?}"
            //  "Driving: $speed mph"
            tileAttribute("device.carState", key: "MARQUEE") {
            	attributeState "Parked", label: '${name}'
                attributeState "Charging", label: '${name}'
                attributeState "Driving", label: '${name}'
            	attributeState "default", label: '${name}', defaultState: true
            }
            // */

            tileAttribute("device.battery", key: "SECONDARY_CONTROL") {
                attributeState("level", label: '${currentValue}%', backgroundColors: [
                        [value: 75, color: "#153591"],
                        [value: 65, color: "#1e9cbb"],
                        [value: 55, color: "#90d2a7"],
                        [value: 45, color: "#44b621"],
                        [value: 35, color: "#f1d801"],
                        [value: 25, color: "#d04e00"],
                        [value: 15, color: "#bc2323"]
                ])
            }

            // FIXME: THERMOSTAT_MODE not supported in generic type
            /*
            tileAttribute("device.thermostatMode", key: "THERMOSTAT_MODE") {
                attributeState("off", label:'Off')
                attributeState("heat", label:'${name}')
                attributeState("cool", label:'${name}')
                attributeState("auto", label:'${name}')
            }
            // */
        }

        valueTile("main", "device.battery", canChangeBackground: true, canChangeIcon: true) {
            state("default", label: '${currentValue}%', defaultState: true,
                    backgroundColors: [
                            [value: 75, color: "#153591"],
                            [value: 65, color: "#1e9cbb"],
                            [value: 55, color: "#90d2a7"],
                            [value: 45, color: "#44b621"],
                            [value: 35, color: "#f1d801"],
                            [value: 25, color: "#d04e00"],
                            [value: 15, color: "#bc2323"]
                    ]
            )
        }
        standardTile("state", "device.sleepState", width: 2, height: 2) {
            // FIXME: onlineState is always online? Never seen that go to any other states.
            //  sleep_state will be "sleeping" when asleep, and then wake action would work.
            state "sleeping", label: "Asleep", backgroundColor: "#eeeeee", action: "wake", icon: "st.Bedroom.bedroom2"
            state "not sleeping", label: "Awake", backgroundColor: "#00a0dc", icon: "st.tesla.tesla-front", defaultState: true, action: "wake" // XXX
            // FIXME: `st.tesla.tesla-front` is not working.
        }
//*
        valueTile("blank_2x1", "device.onlineState", width: 2, height: 1) {
            state 'default', label: '', defaultState: true
        }
        valueTile("help", "device.sleepState", width: 2, height: 1) {
            // what's the point?
            //  'In order to use the various commands, your vehicle must first be awakened.'
            state "sleeping", label: 'Wake Required'
            state "not sleeping", label: '', defaultState: true
        }
        valueTile("lastActivity", "device.lastActivity", width: 2, height: 1) {
            state 'default', label: '${currentValue}', defaultState: true
        }
// */
        standardTile("chargingState", "device.chargingState", width: 2, height: 2) {
            state "default", label: '${currentValue}', icon: "st.Transportation.transportation6", defaultState: true
            state "stopped", label: '${currentValue}', icon: "st.Transportation.transportation6", action: "startCharge", backgroundColor: "#ffffff"
            state "Charging", label: '${currentValue}', icon: "st.Transportation.transportation6", action: "stopCharge", backgroundColor: "#00a0dc"
            state "complete", label: '${currentValue}', icon: "st.Transportation.transportation6", backgroundColor: "#44b621"
        }
        valueTile("battery", "device.battery", width: 2, height: 1) {
            state("default", label: '${currentValue}% battery', defaultState: true
                    /* , backgroundColors:[
                           [value: 75, color: "#153591"],
                           [value: 65, color: "#1e9cbb"],
                           [value: 55, color: "#90d2a7"],
                           [value: 45, color: "#44b621"],
                           [value: 35, color: "#f1d801"],
                           [value: 25, color: "#d04e00"],
                           [value: 15, color: "#bc2323"]
                       ]
                       // */
            )
        }
        valueTile("batteryRange", "device.batteryRange", width: 2, height: 1) {
            state("default", label: '${currentValue} mi range', defaultState: true)
        }
        standardTile("thermostatMode", "device.thermostatMode", width: 2, height: 2) {
            state "auto", label: "On", action: "off", icon: "http://cdn.device-icons.smartthings.com/tesla/tesla-hvac%402x.png", backgroundColor: "#00a0dc"
            state "off", label: "Off", action: "auto", icon: "http://cdn.device-icons.smartthings.com/tesla/tesla-hvac%402x.png", backgroundColor: "#ffffff"
        }
        controlTile("thermostatSetpoint", "device.thermostatSetpoint", "slider", width: 2, height: 2, range: "(60..85)") {
            state "default", action: "setThermostatSetpoint", defaultState: true
        }
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
            state("temperature", label: '${currentValue}°', unit: "dF",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
            )
        }
        valueTile("outsideTemp", "device.temperatureOutside", width: 2, height: 2) {
            state("outsideTemp", label: '${currentValue}°', unit: "dF",
                    backgroundColors: [
                            [value: 31, color: "#153591"],
                            [value: 44, color: "#1e9cbb"],
                            [value: 59, color: "#90d2a7"],
                            [value: 74, color: "#44b621"],
                            [value: 84, color: "#f1d801"],
                            [value: 95, color: "#d04e00"],
                            [value: 96, color: "#bc2323"]
                    ]
            )
        }
        // FIXME: this doesn't work as a 2x2 tile --> always becomes the default 6x4 size
        /*
        multiAttributeTile(name: "outsideTemp_disable", type: "generic", width: 2, height: 2) {
        	tileAttribute("device.temperatureOutside", key: "PRIMARY_CONTROL") {
            	attributeState('outsideTemp', label: '${currentValue}°', unit:"dF", defaultState: true,
                    backgroundColors:[
                        [value: 31, color: "#153591"],
                        [value: 44, color: "#1e9cbb"],
                        [value: 59, color: "#90d2a7"],
                        [value: 74, color: "#44b621"],
                        [value: 84, color: "#f1d801"],
                        [value: 95, color: "#d04e00"],
                        [value: 96, color: "#bc2323"]
                    ])
            }
            tileAttribute("device.temperatureOutside", key: "SECONDARY_CONTROL") {
            	attributeState 'default', label: 'Outside', defaultState: true
            }
        } // */

        standardTile("lock", "device.lock", width: 2, height: 2) {
            state "locked", label: "Locked", action: "unlock", icon: "st.tesla.tesla-locked", backgroundColor: "#00a0dc"
            state "unlocked", label: "Unlocked", action: "lock", icon: "st.tesla.tesla-unlocked", backgroundColor: "#ffffff"
        }

        // FIXME: TeslaFi does not support trunk/door status or actuators
        /*
		valueTile("trunkLabel", "device.onlineState", width: 2, height: 1) {
			state "default", label: 'Open Trunk >', defaultState: true
		}
        standardTile("frontTrunk", "device.onlineState") {
            state "default", label: "Front", action: "openFrontTrunk", defaultState: true
        }
        standardTile("rearTrunk", "device.onlineState") {
        	state "default", label: "Rear", action: "openRearTrunk", defaultState: true
        }
        // */

        standardTile("motion", "device.motion", width: 2, height: 1) {
            // TODO: replaced by multi tile
            state "inactive", label: "Parked", icon: "st.motion.acceleration.inactive"
            state "active", label: "Driving", icon: "st.motion.acceleration.active"
        }
        standardTile("presence", "device.presence", width: 2, height: 1) {
            state "present", label: "Home", icon: "st.presence.house.secured"
            state "not present", label: "Away", icon: "st.presence.car.car"
        }
        valueTile("speed", "device.speed", width: 2, height: 1) {
            state "parked", label: '--'
            state "default", label: '${currentValue} mph', defaultState: true
        }

        standardTile("refresh", "device.onlineState", decoration: "flat", width: 2, height: 2) {
            state "default", action: "refresh.refresh", icon: "st.secondary.refresh", defaultState: true
        }
        valueTile("odometer", "device.odometer", width: 2, height: 1) {
            state "default", label: 'ODO    ${currentValue} mi', defaultState: true
        }
        valueTile("version", "device.version", width: 4, height: 1) {
            state "default", label: '${currentValue}', defaultState: true
        }
        valueTile("vin", "device.vin", width: 4, height: 1) {
            state "default", label: '${currentValue}', defaultState: true
        }

        main("main")
        details(
                "multi", /* 6x4 multi tile */
                "state", "presence", "speed",
                "help", "blank_2x1",
                "chargingState", "battery", "outsideTemp", // TODO: add time-to-full? or range added?
                "batteryRange",       //"blank_2x1", // TODO: add mi/hr rate?
                "thermostatMode", "thermostatSetpoint", "temperature",
                "lock", "version",
                "odometer", "refresh",
                "vin"
        )
    }
}

def initialize() {
    log.debug "Executing 'initialize'"

    sendEvent(name: "supportedThermostatModes", value: ["auto", "off"])

    runIn(2, doRefresh)
    runEvery15Minutes(doRefresh)
}

private processData(data) {
    log.debug "processData: ${data}"
    if (!data) {
        log.error "No data found for ${device.deviceNetworkId}"
        return
    }

    sendEvent(name: "onlineState", value: data.state)
    sendEvent(name: "carState", value: data.car_state)
    sendEvent(name: "sleepState", value: data.sleep_state)
    sendEvent(name: "motion", value: data.motion)
    sendEvent(name: "vin", value: data.vin)
    sendEvent(name: "version", value: data.version)

    if (data.chargeState) {
        // Battery
        sendEvent(name: "battery", value: data.chargeState.battery, unit: '%')
        sendEvent(name: "batteryRange", value: data.chargeState.batteryRange)

        sendEvent(name: "chargingState", value: data.chargeState.chargingState)

        // battery, dc, mains, unknown
        // TODO: when supercharging, powerSource=dc
        if (data.chargeState.chargingState == "Disconnected") {
            sendEvent(name: "powerSource", value: "battery")
        } else if (data.chargeState.fastChargerPresent) {
            // Assuming that fastChargerPresent => Supercharger => DC
            sendEvent(name: "powerSource", value: "dc")
            try {
                // XXX: determine the true nature of these fields
                log.info("JHH: fast* fields: ${data.chargeState}")
                sendEvent(name: "jhh.fastType", value: data.chargeState.fastChargerType)
                sendEvent(name: "jhh.fastBrand", value: data.chargeState.fastChargerBrand)
            } catch (Exception e) {
                log.error("JHH: unable to emit fast* events", e)
            }
        } else {
            sendEvent(name: "powerSource", value: "mains")
        }

        sendEvent(name: "energy", value: data.chargeState.chargeEnergyAdded, unit: 'kWh')
        sendEvent(name: "voltage", value: data.chargeState.chargerVoltage, unit: 'V')
        sendEvent(name: "power", value: data.chargeState.chargerPower, unit: 'kW') // kW to W

        if (data.chargeState.chargingState == "Charging") {
            sendEvent(name: "chargeTimeRemaining", value: data.chargeState.hoursRemaining, unit: 'h')

            // Timed Session
            if (data.chargeState.hoursRemaining != null) {
                def minutesRemaining = (data.chargeState.hoursRemaining as float) * 60
                def eta = new GregorianCalendar()
                eta.add(Calendar.MINUTE, Math.round(minutesRemaining as float))
                log.info "JHH New completion time: ${eta.time}"
                sendEvent(name: "completionTime", value: eta.time)
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
            sendEvent(name: "speed", value: "parked")
        } else {
            sendEvent(name: "speed", value: data.driveState.speed, unit: "mph")
        }
        sendEvent(name: "latitude", value: data.driveState.latitude)
        sendEvent(name: "longitude", value: data.driveState.longitude)
        sendEvent(name: "method", value: data.driveState.method)
        sendEvent(name: "heading", value: data.driveState.heading)
        // lastUpdateTime = gps_as_of (unix timestamp)
        sendEvent(name: "lastUpdateTime", value: data.driveState.lastUpdateTime)
    }

    if (data.vehicleState) {
        sendEvent(name: "presence", value: data.vehicleState.presence)
        sendEvent(name: "lock", value: data.vehicleState.lock)
        sendEvent(name: "odometer", value: data.vehicleState.odometer)
    }

    if (data.climateState) {
        sendEvent(name: "temperature", value: data.climateState.temperature)
        sendEvent(name: "thermostatSetpoint", value: data.climateState.thermostatSetpoint)
        sendEvent(name: "thermostatMode", value: data.climateState.thermostatMode)
        // TODO: needs another child device? Already using Temperature Measurement for inside temp...
        sendEvent(name: "temperatureOutside", value: data.climateState.outsideTemp)
    }
}

def doRefresh() {
    log.debug "Refreshing car data now; last update=${device.getLastActivity()}"
    def data = parent.refresh(this)
    processData(data)

    if (data?.car_state == 'Driving') {
        log.debug "Refreshing more often because Driving"
        runEvery5Minutes(refreshWhileDriving)
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
    log.debug "Executing 'auto'"
    def result = parent.climateAuto(this)
    if (result) doRefresh()
}

def off() {
    log.debug "Executing 'off'"
    def result = parent.climateOff(this)
    if (result) doRefresh()
}

def heat() {
    log.info "Executing 'heat' - Not supported"
    // Not supported
}

def emergencyHeat() {
    log.info "Executing 'emergencyHeat' - Not supported"
    // Not supported
}

def cool() {
    log.info "Executing 'cool' - Not supported"
    // Not supported
}

def setThermostatMode(mode) {
    log.debug "Executing 'setThermostatMode'"
    switch (mode) {
        case "auto":
            auto()
            break
        case "off":
            off()
            break
        default:
            log.error "setThermostatMode: Only thermostat modes Auto and Off are supported"
    }
}

def setThermostatSetpoint(setpoint) {
    log.debug "Executing 'setThermostatSetpoint'"
    def result = parent.setThermostatSetpoint(this, setpoint)
    if (result) doRefresh()
}

def startCharge() {
    log.debug "Executing 'startCharge'"
    def result = parent.startCharge(this)
    if (result) doRefresh()
}

def stopCharge() {
    log.debug "Executing 'stopCharge'"
    def result = parent.stopCharge(this)
    if (result) doRefresh()
}
