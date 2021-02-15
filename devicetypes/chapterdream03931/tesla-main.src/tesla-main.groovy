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
            name: "Tesla (Main)",
            namespace: "chapterdream03931",
            author: "Joe Hansche",
            mnmn: "SmartThingsCommunity",
            // ocfDeviceType: "x.com.st.d.tesla",
            ocfDeviceType: "oic.d.vehicleconnector",
            vid: "d487bc57-082d-3b19-873c-a0c4899877bc"
    ) {
        // .vin, .hwVersion, .swVersion, .odometerMiles,
        // .carState = [Sleeping | Idling | Driving]
        // FIXME: do we also need .ev.carState = [Charging, Sentry]?
        //  otherwise maybe just ev.chargingState is enough? and move Sentry=Alarm?
        // wake, refresh
        capability "chapterdream03931.automobile"

        capability "Geolocation" // lat/long, speed, heading
        capability "Lock" // .locked
        capability "Motion Sensor" // .motion = active | inactive
        capability "Presence Sensor" // .presence = [present, not present]
        capability "Refresh" // refresh()
        capability "Sleep Sensor" // .sleeping = [sleeping, not_sleeping]
        capability "Tone" // use it for the horn?

        // FIXME: move these to capability
        attribute "onlineState", "string"
    }
}

def initialize() {
    log.debug "Executing 'initialize'"

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
        if (device.currentValue("odometerMiles")?.toFloat() != data.vehicleState.odometer) {
            sendEvent(name: "odometerMiles", value: data.vehicleState.odometer, unit: 'mi')
        }
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