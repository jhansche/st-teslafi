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
            name: "Tesla (Climate)",
            namespace: "chapterdream03931",
            author: "Joe Hansche",
            mnmn: "SmartThingsCommunity",
            // ocfDeviceType: "x.com.st.d.tesla",
            ocfDeviceType: "oic.d.vehicleconnector",
            vid: "d487bc57-082d-3b19-873c-a0c4899877bc"
    ) {
        capability "Refresh" // refresh()
        capability "Temperature Measurement"
        capability "Thermostat Mode" // .thermostatMode = [auto, off]
        capability "Thermostat Setpoint"

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
    log.debug "processData for Climate: ${data}"
    if (!data) {
        log.error "No data found for ${device.deviceNetworkId}"
        return
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
}

// Only use this for the manual refresh tile action
def refresh() {
    log.debug "Triggering refresh by command"
    doRefresh()
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