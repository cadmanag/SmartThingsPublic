/**
 *  Notify when no events
 *
 *  Copyright 2015 Bruce Ravenel
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
    name: "Refresh & Notify when no events",
    namespace: "bravenel",
    author: "Bruce Ravenel",
    description: "Notify when a device doesn't wake up",
    category: "My Apps",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png")


preferences {
	section("When one of these sensors fails to wake up...") {
    	input "devices", "capability.Refresh", title:"Devices to watch", multiple: true
	}
    section("refresh after this many minutes") {
    	input "rminutes", "number", title:"Maximum minutes of no events"
    }
    section("notify after this many minutes") {
    	input "nminutes", "number", title:"Maximum minutes of no events"
    }
    section("Send sms (leave blank for push)") {
    	input "phoneNumber", "phone", title: "Phone number", required: false 
    }
}

def installed() {
	log.debug "Inst..."
	unschedule("sched")
	sched()
	initialize()
}

def updated() {
	log.debug "Upd..."
	unschedule("sched")
	sched()
	initialize()
}

def sched() {
	log.debug "Sched..."
	unschedule("checkSensorStatus")
    runEvery15Minutes(checkSensorStatus)
}

def initialize() {
	log.debug "Init..."
    schedule(now() - 120000, sched)
    //runPeriodically(3600, poll)
    runIn(61,updateSensorStatus)
}

def send(msg) {
	log.debug "$msg"
    if (phoneNumber) sendSms(phoneNumber, msg) 
    else sendPush(msg)
}

def updateSensorStatus() {
	checkSensorStatus()
}

def checkSensorStatus() {
    devices.each {
    	def lastTime = it.events(max: 1).date
        def rightNow = new Date()
        log.debug "Checking: $it.displayName for time within $rminutes minutes."
        log.debug "Last Time event: $lastTime"
        if(lastTime) {
        	def minutes = ((rightNow.time - lastTime.time) / 60000)
            if(minutes < 0) minutes = minutes + 1440
            if(minutes > rminutes) {
                it.refresh()
        		log.info "No events in $minutes minutes, sent refresh to $it.displayName"
            	//send("No events for $it.displayName in over $hours hours")
                def rlastTime = it.events(max: 1).date
                if(rlastTime == lastTime) log.info "Refreshes failing, $rminutes since last refresh..."
                def rrightNow = new Date()
       		 	def rminutes = ((rrightNow.time - rlastTime.time) / 60000)           	
            	if(rminutes < 0) rminutes = rminutes + 1440
        		if(rminutes > nminutes) send("No events for $it.displayName in over $rminutes minutes, exceeding $nminutes.")
            } else {
            	if(minutes > nminutes) send("No events for $it.displayName in over $minutes minutes, exceeding $nminutes.")
            }
        } else {
        	it.refresh()
       		log.debug "Sent refresh, no recent event."
            def rlastTime = it.events(max: 1).date
            if (rlastTime) {
            	send("$it.displayName had no recent events, refreshed at $rlastTime")
            } else {
        		send("Unable to refresh, no recent events for $it.displayName")
            }
        }
    }
}