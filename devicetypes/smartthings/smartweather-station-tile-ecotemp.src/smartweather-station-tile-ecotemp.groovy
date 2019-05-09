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
    definition (name: "SmartWeather Station Tile - EcoTemp", namespace: "smartthings", author: "SmartThings") {
        capability "Illuminance Measurement"
        capability "Temperature Measurement"
        capability "Relative Humidity Measurement"
        capability "Ultraviolet Index"
        capability "Sensor"
        capability "Refresh"

        attribute "localSunrise", "string"
        attribute "localSunset", "string"
        attribute "city", "string"
        attribute "timeZoneOffset", "string"
        attribute "weather", "string"
        attribute "wind", "string"
        attribute "windVector", "string"
        attribute "weatherIcon", "string"
        attribute "forecastIcon", "string"
        attribute "feelsLike", "string"
        attribute "dewPoint", "string"
        attribute "percentPrecip", "string"
        attribute "alert", "string"
        attribute "alertKeys", "string"
        attribute "sunriseDate", "string"
        attribute "sunsetDate", "string"
        attribute "lastUpdate", "string"
        attribute "uvDescription", "string"
        attribute "forecastToday", "string"
        attribute "forecastTonight", "string"
        attribute "forecastTomorrow", "string"
    }

    preferences {
        input "zipCode", "text", title: "Zip Code (optional)", required: false
        input "stationId", "text", title: "Personal Weather Station ID (optional)", required: false
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
    poll()
    runEvery30Minutes(poll)
}

def uninstalled() {
    unschedule()
}

// handle commands
def poll() {
    log.info "WUSTATION: Executing 'poll', location: ${location.name}"
    if (stationId) {
        pollUsingPwsId(stationId.toUpperCase())
    } else {
        if (zipCode && zipCode.toUpperCase().startsWith('PWS:')) {
            log.debug zipCode.substring(4)
            pollUsingPwsId(zipCode.substring(4).toUpperCase())
        } else {
            pollUsingZipCode(zipCode?.toUpperCase())
        }
    }
}

def pollUsingZipCode(String zipCode) {
    // Last update time stamp
    def timeZone = location.timeZone ?: timeZone(timeOfDay)
    def timeStamp = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    sendEvent(name: "lastUpdate", value: timeStamp)

    // Current conditions
    def tempUnits = getTemperatureScale()
    def windUnits = tempUnits == "C" ? "KPH" : "MPH"
    def obs = getTwcConditions(zipCode)
    if (obs) {
        // TODO def weatherIcon = obs.icon_url.split("/")[-1].split("\\.")[0]
		log.debug "tempUnits: $tempUnits"
        double myHumidFeelAdd
        double myWindFeelDrop
        double first
		if(tempUnits == "F") {
        	// int humidity = Integer.parseInt(obs.relative_humidity[0..-2])
            // int dewpoint_c = obs.temp_c - ((100 - humidity) / 5)
            // def dewpoint_f = (dewpoint_c * 9 / 5) + 32
            int dewpoint_f = obs.temperatureDewPoint
            log.debug "dewpoint_f: $dewpoint_f"
            if (dewpoint_f < 49) {
                myHumidFeelAdd = 0
            } else {
                first = ((dewpoint_f - 49) / 3.5) ** 1.8
                myHumidFeelAdd = Math.round((28 * first) / (first + 21))
            }
            log.debug "myHumidFeelAdd: $myHumidFeelAdd"
            int wind_mph = obs.windSpeed
            log.debug "wind_mph: $wind_mph"
            myWindFeelDrop
            if (wind_mph == 0) {
                myWindFeelDrop = 0
            } else {
                myWindFeelDrop = Math.round((8 * wind_mph) / (wind_mph + 5))
            }
            log.debug "myWindFeelDrop: $myWindFeelDrop"
        }
        if(tempUnits == "C") {
        	int dewpoint_c = obs.temperatureDewPoint
            log.debug "dewpoint_c: $dewpoint_c"
            if (dewpoint_c < 9) {
                myHumidFeelAdd = 0
            } else {
                first = ((dewpoint_c - 9) / 2) ** 1.8
                myHumidFeelAdd = Math.round((14 * first) / (first + 11))
            }
            log.debug "myHumidFeelAdd: $myHumidFeelAdd"
            int wind_kph = obs.windSpeed
            log.debug "wind_kph: $wind_kph"
            if (wind_mph == 0) {
                myWindFeelDrop = 0
            } else {
                myWindFeelDrop = Math.round((4 * wind_mph) / (wind_mph + 10))
            }
            log.debug "myWindFeelDrop: $myWindFeelDrop"
        }
        log.debug "obs.temperature $obs.temperature"
        int theTemp = obs.temperature
        log.debug "theTemp $theTemp"
        int myFeelsLike = Math.round(theTemp + myHumidFeelAdd - myWindFeelDrop)
        log.debug "myFeelsLike $myFeelsLike"

        send(name: "temperatureActual", value: theTemp, unit: tempUnits)
        send(name: "temperature", value: myFeelsLike, unit: tempUnits)
        send(name: "feelsLike", value: obs.temperatureFeelsLike, unit: tempUnits)
        send(name: "myFeelsLike", value: myFeelsLike, unit: tempUnits)
        log.debug "obs.temperatureDewPoint $obs.temperatureDewPoint"
		send(name: "dewPoint", value: obs.temperatureDewPoint, unit: tempUnits)
        send(name: "humidity", value: obs.relativeHumidity, unit: "%")
        send(name: "weather", value: obs.wxPhraseShort)
        send(name: "weatherIcon", value: obs.iconCode as String, displayed: false)
        send(name: "wind", value: obs.windSpeed as String, unit: windUnits) // as String because of bug in determining state change of 0 numbers
        send(name: "windVector", value: "${obs.windDirectionCardinal} ${obs.windSpeed} ${windUnits}")
        log.trace "Getting location info"
        def loc = getTwcLocation(zipCode).location
        def cityValue = "${loc.city}, ${loc.adminDistrictCode} ${loc.countryCode}"
        if (cityValue != device.currentValue("city")) {
            send(name: "city", value: cityValue, isStateChange: true)
        }

        send(name: "ultravioletIndex", value: obs.uvIndex)
        send(name: "uvDescription", value: obs.uvDescription)

        def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")

        def sunriseDate = dtf.parse(obs.sunriseTimeLocal)
        log.info "'${obs.sunriseTimeLocal}'"

        def sunsetDate = dtf.parse(obs.sunsetTimeLocal)

        def tf = new java.text.SimpleDateFormat("h:mm a")
        tf.setTimeZone(TimeZone.getTimeZone(loc.ianaTimeZone))

        def localSunrise = "${tf.format(sunriseDate)}"
        def localSunset = "${tf.format(sunsetDate)}"
        send(name: "localSunrise", value: localSunrise, descriptionText: "Sunrise today is at $localSunrise")
        send(name: "localSunset", value: localSunset, descriptionText: "Sunset today at is $localSunset")

        send(name: "illuminance", value: estimateLux(obs, sunriseDate, sunsetDate))

        // Forecast
        def f = getTwcForecast(zipCode)
        if (f) {
            def icon = f.daypart[0].iconCode[0] ?: f.daypart[0].iconCode[1]
            def value = f.daypart[0].precipChance[0] ?: f.daypart[0].precipChance[1]
            def narrative = f.daypart[0].narrative
            send(name: "percentPrecip", value: value, unit: "%")
            send(name: "forecastIcon", value: icon, displayed: false)
            send(name: "forecastToday", value: narrative[0])
            send(name: "forecastTonight", value: narrative[1])
            send(name: "forecastTomorrow", value: narrative[2])
        }
        else {
            log.warn "Forecast not found"
        }

        // Alerts
        def alerts = getTwcAlerts("${loc.latitude},${loc.longitude}")
        if (alerts) {
            alerts.each {alert ->
                def msg = alert.headlineText
                if (alert.effectiveTimeLocal && !msg.contains(" from ")) {
                    msg += " from ${parseAlertTime(alert.effectiveTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.effectiveTimeLocalTimeZone))}"
                }
                if (alert.expireTimeLocal && !msg.contains(" until ")) {
                    msg += " until ${parseAlertTime(alert.expireTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.expireTimeLocalTimeZone))}"
                }
                send(name: "alert", value: msg, descriptionText: msg)
            }
        }
        else {
            send(name: "alert", value: "No current alerts", descriptionText: msg)
        }
    }
    else {
        log.warn "No response from TWC API"
    }

    return null
}

def pollUsingPwsId(String stationId) {
    // Last update time stamp
    def timeZone = location.timeZone ?: timeZone(timeOfDay)
    def timeStamp = new Date().format("yyyy MMM dd EEE h:mm:ss a", location.timeZone)
    sendEvent(name: "lastUpdate", value: timeStamp)

    // Current conditions
    def tempUnits = getTemperatureScale()
    def windUnits = tempUnits == "C" ? "KPH" : "MPH"
    def obsWrapper = getTwcPwsConditions(stationId)
    if (obsWrapper && obsWrapper.observations && obsWrapper.observations.size()) {
        def obs = obsWrapper.observations[0]
        def dataScale = obs.imperial ? 'imperial' : 'metric'
        send(name: "temperature", value: convertTemperature(obs[dataScale].temp, dataScale, tempUnits), unit: tempUnits)
        send(name: "feelsLike", value: convertTemperature(obs[dataScale].windChill, dataScale, tempUnits), unit: tempUnits)
		send(name: "dewPoint", value: convertTemperature(obs[dataScale].dewpt, dataScale, tempUnits), unit: tempUnits)
        
        send(name: "humidity", value: obs.humidity, unit: "%")
        send(name: "weather", value: "n/a")
        send(name: "weatherIcon", value: null as String, displayed: false)
        send(name: "wind", value: convertWindSpeed(obs[dataScale].windSpeed, dataScale, tempUnits) as String, unit: windUnits) // as String because of bug in determining state change of 0 numbers
        send(name: "windVector", value: "${obs.winddir}° ${convertWindSpeed(obs[dataScale].windSpeed, dataScale, tempUnits)} ${windUnits}")
        def cityValue = obs.neighborhood
        if (cityValue != device.currentValue("city")) {
            send(name: "city", value: cityValue, isStateChange: true)
        }

        send(name: "ultravioletIndex", value: obs.uv)
        send(name: "uvDescription", value: "n/a")

        send(name: "localSunrise", value: "n/a", descriptionText: "Sunrise is not supported when using PWS")
        send(name: "localSunset", value: "n/a", descriptionText: "Sunset is not supported when using PWS")
        send(name: "illuminance", value: null)

        // Forecast not supported
        send(name: "percentPrecip", value: "n/a", unit: "%")
        send(name: "forecastIcon", value: null, displayed: false)
        send(name: "forecastToday", value: "n/a")
        send(name: "forecastTonight", value: "n/a")
        send(name: "forecastTomorrow", value: "n/a")
        log.warn "Forecast not supported when using PWS"

        // Alerts
        def alerts = getTwcAlerts("${obs.lat},${obs.lon}")
        if (alerts) {
            alerts.each {alert ->
                def msg = alert.headlineText
                if (alert.effectiveTimeLocal && !msg.contains(" from ")) {
                    msg += " from ${parseAlertTime(alert.effectiveTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.effectiveTimeLocalTimeZone))}"
                }
                if (alert.expireTimeLocal && !msg.contains(" until ")) {
                    msg += " until ${parseAlertTime(alert.expireTimeLocal).format("E hh:mm a", TimeZone.getTimeZone(alert.expireTimeLocalTimeZone))}"
                }
                send(name: "alert", value: msg, descriptionText: msg)
            }
        }
        else {
            send(name: "alert", value: "No current alerts", descriptionText: msg)
        }
    }
    else {
        log.warn "No response from TWC API"
    }

    return null
}

def parseAlertTime(s) {
    def dtf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
    def s2 = s.replaceAll(/([0-9][0-9]):([0-9][0-9])$/,'$1$2')
    dtf.parse(s2)
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

private estimateLux(obs, sunriseDate, sunsetDate) {
    def lux = 0
    if (obs.dayOrNight == 'N') {
        lux = 10
    }
    else {
        //day
        switch(obs.iconCode) {
            case '04':
                lux = 200
                break
            case ['05', '06', '07', '08', '09', '10',
                  '11', '12', '13','14', '15','17','18','19','20',
                  '21','22','23','24','25','26']:
                lux = 1000
                break
            case ['27', '28']:
                lux = 2500
                break
            case ['29', '30']:
                lux = 7500
                break
            default:
                //sunny, clear
                lux = 10000
        }

        //adjust for dusk/dawn
        def now = new Date().time
        def afterSunrise = now - sunriseDate.time
        def beforeSunset = sunsetDate.time - now
        def oneHour = 1000 * 60 * 60

        if(afterSunrise < oneHour) {
            //dawn
            lux = (long)(lux * (afterSunrise/oneHour))
        } else if (beforeSunset < oneHour) {
            //dusk
            lux = (long)(lux * (beforeSunset/oneHour))
        }
    }
    lux
}

private fixScale(scale) {
    switch (scale.toLowerCase()) {
        case "c":
        case "metric":
            return "metric"
        default:
            return "imperial"
    }
}

private convertTemperature(value, fromScale, toScale) {
    def fs = fixScale(fromScale)
    def ts = fixScale(toScale)
    if (fs == ts) {
        return value
    }
    if (ts == 'imperial') {
        return value * 9.0 / 5.0 + 32.0
    }
    return (value - 32.0) * 5.0 / 9.0
}

private convertWindSpeed(value, fromScale, toScale) {
    def fs = fixScale(fromScale)
    def ts = fixScale(toScale)
    if (fs == ts) {
        return value
    }
    if (ts == 'imperial') {
        return value * 1.608
    }
    return value / 1.608
}