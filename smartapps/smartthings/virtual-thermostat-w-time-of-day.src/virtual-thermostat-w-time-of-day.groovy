/**
 *  Copyright 2015 SmartThings
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
 *  Virtual Thermostat
 *
 *  Author: SmartThings
 */
definition(
    name: "Virtual Thermostat w/ Time of Day",
    namespace: "smartthings",
    author: "SmartThings",
    description: "Control a space heater or window air conditioner in conjunction with any temperature sensor, like a SmartSense Multi.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/temp_thermo-switch@2x.png"
)

preferences {
	section("Choose a temperature sensor... "){
		input "sensor", "capability.temperatureMeasurement", title: "Sensor"
	}
	section("Select the heater or air conditioner outlet(s)... "){
		input "outlets", "capability.switch", title: "Outlets", multiple: true
	}
	section("Set the desired temperature..."){
		input "setpoint", "decimal", title: "Set Temp"
	}
    section("Only during a certain time...") {
			input "starting", "time", title: "Starting", required: false
			input "ending", "time", title: "Ending", required: false
	}
    section("Manual ON during a certain time...") {
			input "mstarting", "time", title: "Starting", required: false
			input "mending", "time", title: "Ending", required: false
	}
	section("When there's been movement from (optional, leave blank to not require motion)..."){
		input "motion", "capability.motionSensor", title: "Motion", required: false
	}
	section("Within this number of minutes..."){
		input "minutes", "number", title: "Minutes", required: false
	}
	section("But never go below (or above if A/C) this value with or without motion..."){
		input "emergencySetpoint", "decimal", title: "Emer Temp", required: false
	}
	section("Select 'heat' for a heater and 'cool' for an air conditioner..."){
		input "mode", "enum", title: "Heating or cooling?", options: ["Heat","Cool"]
	}
}

def installed()
{
	subscribe(sensor, "temperature", temperatureHandler)
	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
}

def updated()
{
	unsubscribe()
	subscribe(sensor, "temperature", temperatureHandler)
	if (motion) {
		subscribe(motion, "motion", motionHandler)
	}
}

def temperatureHandler(evt)
{
	def isActive = hasBeenRecentMotion()
    log.trace "Temp Change (Recent Motion: $isActive)"
	if (isActive || emergencySetpoint) {
		evaluate(evt.doubleValue, isActive ? setpoint : emergencySetpoint)
	}
	else {
		outlets.off()
	}
}

def motionHandler(evt)
{
	if (evt.value == "active") {
    	log.trace "Movement..."
		def lastTemp = sensor.currentTemperature
		if (lastTemp != null) {
			evaluate(lastTemp, setpoint)
		}
	} else if (evt.value == "inactive") {
		def isActive = hasBeenRecentMotion()
		log.debug "INACTIVE-Recent($isActive)"
		if (isActive || emergencySetpoint) {
			def lastTemp = sensor.currentTemperature
			if (lastTemp != null) {
				evaluate(lastTemp, isActive ? setpoint : emergencySetpoint)
			}
		}
		else {
        	log.trace "No recent movement..."
			outlets.off()
		}
	}
}

private evaluate(currentTemp, desiredTemp)
{
	log.debug "EVALUATE($currentTemp, $desiredTemp)"
	def threshold = 1.0
    def isTimeOk = myTimeOk()
    def isManualTimeOk = myManualTimeOk()
	if (mode == "Cool") {
		// air conditioner
		if (currentTemp - desiredTemp >= threshold) {
        	log.trace "Too hot..."
			if (isTimeOk) {outlets.on()} else { if (isManualTimeOk) {} else { outlets.off()}}
		}
		else if (desiredTemp - currentTemp >= threshold) {
        	log.trace "Cool enough..."
			outlets.off()
		}
	}
	else {
		// heater
		if (desiredTemp - currentTemp >= threshold) {
        	log.trace "Too cold..."
			if (isTimeOk) {outlets.on()} else { if (isManualTimeOk) {} else { outlets.off()}}
		}
		else if (currentTemp - desiredTemp >= threshold) {
        	log.trace "Warm enough..."
			outlets.off()
		}
	}
}

private hasBeenRecentMotion()
{
	def isActive = false
        if (motion && minutes) {
            def deltaMinutes = minutes as Long
            if (deltaMinutes) {
                def motionEvents = motion.eventsSince(new Date(now() - (60000 * deltaMinutes)))
                log.trace "Found ${motionEvents?.size() ?: 0} events in the last $deltaMinutes minutes"
                if (motionEvents.find { it.value == "active" }) {
                    isActive = true
                }
            }
        }
        else {
            isActive = true
        }
	isActive
}

def myTimeOk() {
	def result = true
	if (starting && ending) {
		def currTime = now()
		def start = timeToday(starting, location?.timeZone).time
		def stop = timeToday(ending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOk = $result"
	result
}
def myManualTimeOk() {
	def result = false
	if (mstarting && mending) {
		def currTime = now()
		def start = timeToday(mstarting, location?.timeZone).time
		def stop = timeToday(mending, location?.timeZone).time
		result = start < stop ? currTime >= start && currTime <= stop : currTime <= stop || currTime >= start
	}
	log.trace "timeOkManual = $result"
	result
}