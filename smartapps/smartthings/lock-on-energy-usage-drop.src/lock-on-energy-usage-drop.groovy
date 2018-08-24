/**
 *  Energy Saver
 *
 *  Copyright 2014 SmartThings
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
definition(
    name: "Lock on Energy Usage drop",
    namespace: "smartthings",
    author: "SmartThings",
		description: "Lock when power usage decreases.",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Meta/light_outlet@2x.png"
)

preferences {
	section {
		input(name: "meter", type: "capability.powerMeter", title: "When This Power Meter...", required: true, multiple: false, description: null)
        input(name: "threshold", type: "number", title: "Reports Below...", required: true, description: "in either watts or kw.")
	}
    section("Lock the lock...") {
		input "lock1","capability.lock", multiple: true, required: true
		input "unlock", "enum", title: "Unlock when when energy increases?", options: ["Yes","No"]
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"
	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"
	unsubscribe()
	initialize()
}

def initialize() {
	subscribe(meter, "power", meterHandler)
}

def meterHandler(evt) {
    def meterValue = evt.value as double
    def thresholdValue = threshold as int
    if (meterValue > thresholdValue) {
        if (unlock == "Yes") {
        	log.debug "${meter} reported energy consumption of ${meterValue} above ${threshold}. Unlocking."
        	lock1.unlock()
        }
    }
    if (meterValue < thresholdValue) {
	    log.debug "${meter} reported energy consumption of ${meterValue} below ${threshold}. Locking."
        lock1.lock()
    }
}
