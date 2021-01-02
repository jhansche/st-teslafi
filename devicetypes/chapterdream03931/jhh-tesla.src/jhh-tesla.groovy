metadata {
    definition(
        name: "jhh-tesla", 
        namespace: "chapterdream03931", 
        author: "Joe Hansche",
        mnmn: "SmartThingsCommunity",
        // vid: "eb904bfe-0657-4f7e-b85b-00e3cae39280"
        vid: "c365fc57-fd43-33e6-b299-1837e83b40fc"
    ) {
        // vin, speed, hwVer, swVer, geo, heading, odometer, carState
        // wake, refresh
        capability "chapterdream03931.automobile"
        capability "chapterdream03931.electricVehicle"
        capability "Battery"
        capability "Refresh"
    }
}

def initialize() {
    log.debug "Executing 'initialize'"
}

// Only use this for the manual refresh tile action
def refresh() {
    log.debug "Triggering refresh by command"
    sendEvent(name: "chapterdream03931.automobile.vin", value: "jhh")
}