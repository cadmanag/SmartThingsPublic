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
 *  SmartWeather Station
 *
 *  Author: SmartThings
 *
 *  Date: 2013-04-30
 */
metadata {
	definition (name: "SmartWeather - EcoTemp", namespace: "smartthings", author: "SmartThings") {
		capability "Temperature Measurement"
		capability "Relative Humidity Measurement"
		capability "Sensor"
        capability "Refresh"

		attribute "city", "string"
		attribute "timeZoneOffset", "string"
		attribute "feelsLike", "string"
        attribute "temperatureActual", "string"
        attribute "dewPoint", "string"
		attribute "percentPrecip", "string"

		command "refresh"
	}

	preferences {
		input "zipCode", "text", title: "Zip Code (optional)", required: false
	}

	tiles {
        valueTile("temperature", "device.temperature", width: 2, height: 2) {
			state "default", label:'Eco AC ${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
		}

		valueTile("humidity", "device.humidity", decoration: "flat") {
			state "default", label:'${currentValue}% humidity'
		}
		valueTile("feelsLike", "device.feelsLike") {
			state "default", label:'feels like ${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
		}
        valueTile("dewPoint", "device.dewPoint") {
			state "default", label:'Dew Pt ${currentValue}°',
            backgroundColors:[
					[value: 31, color: "#1e9cbb"],
					[value: 46, color: "#90d2a7"],
					[value: 56, color: "#44b621"],
					[value: 61, color: "#f1d801"],
					[value: 66, color: "#d05f00"],
					[value: 71, color: "#f13801"],
					[value: 76, color: "#bc2323"]
				]
		}

		valueTile("city", "device.city", decoration: "flat") {
			state "default", label:'${currentValue}'
		}

		standardTile("refresh", "device.weather", decoration: "flat", width: 2, height: 1) {
			state "default", label: "", action: "refresh", icon:"st.secondary.refresh"
		}

        valueTile("statusText", "device.statusText", width: 2, height: 1, decoration: "flat") {
			state "default", label:'${currentValue}'
		}

		valueTile("temperatureActual", "device.temperatureActual") {
			state "default", label:'Temp   ${currentValue}°',
				backgroundColors:[
					[value: 31, color: "#153591"],
					[value: 44, color: "#1e9cbb"],
					[value: 59, color: "#90d2a7"],
					[value: 74, color: "#44b621"],
					[value: 84, color: "#f1d801"],
					[value: 95, color: "#d04e00"],
					[value: 96, color: "#bc2323"]
				]
		}

		main(["temperature","dewPoint"])
		details(["temperature", "dewPoint","temperatureActual", "refresh","humidity","city","statusText"])}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
}

def installed() {
	runPeriodically(3600, poll)
}

def uninstalled() {
	unschedule()
}

def updated() {
	runPeriodically(3600, poll)
}

// handle commands
def poll() {
	log.debug "WUSTATION: Executing 'poll', location: ${location.name}"
    def statusTextmsg = ""
    def timeString = new Date().format("h:mma MM-dd-yyyy", location.timeZone)
    statusTextmsg = "Last active: "+timeString
    send(name: "statusText", value: statusTextmsg)
    log.debug "Status Text: $statusTextmsg"
    def lastTime = device.events(max: 1).date
    log.debug "Last event $lastTime"

	// Current conditions
	def obs = get("conditions")?.current_observation
	if (obs) {
		def weatherIcon = obs.icon_url.split("/")[-1].split("\\.")[0]

        // int humidity = Integer.parseInt(obs.relative_humidity[0..-2])
        // int dewpoint_c = obs.temp_c - ((100 - humidity) / 5)
        // def dewpoint_f = (dewpoint_c * 9 / 5) + 32
        int dewpoint_f = obs.dewpoint_f
        log.debug "dewpoint_f: $dewpoint_f"
        int myHumidFeelAdd
        if (dewpoint_f < 49) {
            myHumidFeelAdd = 0
        } else {
            double first = ((dewpoint_f - 49) / 3.5) ** 1.8
            myHumidFeelAdd = Math.round((28 * first) / (first + 21))
        }
    	log.debug "myHumidFeelAdd: $myHumidFeelAdd"
        int wind_mph = obs.wind_mph
        log.debug "wind_mph: $wind_mph"
        int myWindFeelDrop
        if (wind_mph == 0) {
            myWindFeelDrop = 0
        } else {
            myWindFeelDrop = Math.round((8 * wind_mph) / (wind_mph + 5))
        }
    	log.debug "myWindFeelDrop: $myWindFeelDrop"
		if(getTemperatureScale() == "C") {
			send(name: "temperatureActual", value: Math.round(obs.temp_c), unit: "C")
			send(name: "feelsLike", value: Math.round(obs.feelslike_c as Double), unit: "C")
            send(name: "dewPoint", value: Math.round(obs.dewpoint_c as Double), unit: "C")
            myHumidFeelAdd = (myHumidFeelAdd * 9 / 5) + 32
            myWindFeelDrop = (myWindFeelDrop * 9 / 5) + 32
            send(name: "temperature", value: Math.round(obs.temp_c + myHumidFeelAdd - myWindFeelDrop), unit: "F")
		} else {
			send(name: "temperatureActual", value: Math.round(obs.temp_f), unit: "F")
			send(name: "feelsLike", value: Math.round(obs.feelslike_f as Double), unit: "F")
            send(name: "dewPoint", value: Math.round(obs.dewpoint_f as Double), unit: "F")
            send(name: "temperature", value: Math.round(obs.temp_f + myHumidFeelAdd - myWindFeelDrop), unit: "F")
		}
		
		send(name: "humidity", value: obs.relative_humidity[0..-2] as Integer, unit: "%")

		def cityValue = "${obs.display_location.city}, ${obs.display_location.state}"
		if (cityValue != device.currentValue("city")) {
			send(name: "city", value: cityValue, isStateChange: true)
		}

		
	}
	else {
		log.warn "No response from Weather Underground API"
	}
}

def refresh() {
	poll()
}

def configure() {
	poll()
}

private pad(String s, size = 25) {
	def n = (size - s.size()) / 2
	if (n > 0) {
		def sb = ""
		n.times {sb += " "}
		sb += s
		n.times {sb += " "}
		return sb
	}
	else {
		return s
	}
}


private get(feature) {
	getWeatherFeature(feature, zipCode)
}

private localDate(timeZone) {
	def df = new java.text.SimpleDateFormat("yyyy-MM-dd")
	df.setTimeZone(TimeZone.getTimeZone(timeZone))
	df.format(new Date())
}

private send(map) {
	log.debug "WUSTATION: event: $map"
	sendEvent(map)
}

