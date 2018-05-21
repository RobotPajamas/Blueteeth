"use strict";
exports.__esModule = true;
var bleno = require("bleno");
var counter_characteristic_1 = require("./counter-characteristic");
var peripheralName = "Blueteeth";
var serviceUuid = "00726f626f7470616a616d61732e6361";
var serviceUuids = [serviceUuid];
bleno.on("stateChange", function (state) {
    console.log("Change state -> " + state);
    if (state === "poweredOn") {
        bleno.startAdvertising(peripheralName, serviceUuids, function (error) {
            if (error) {
                console.log("startAdvertising: Advertising started with error: " + error);
            }
        });
    }
    else {
        bleno.stopAdvertising(function (error) {
            if (error) {
                console.log("stopAdvertising: Advertising stopped with error: " + error);
            }
        });
    }
});
bleno.on("advertisingStart", function (error) {
    if (error) {
        console.log("onAdvertisingStart: Advertising error -> " + error);
    }
    else {
        console.log("onAdvertisingStart: Advertising started -> name: " + peripheralName);
        console.log("onAdvertisingStart: Services running -> \n\t" + serviceUuids.join("\n\t"));
        bleno.setServices([
            new bleno.PrimaryService({
                characteristics: [
                    new counter_characteristic_1.CounterCharacteristic("01726f626f7470616a616d61732e6361"),
                ],
                uuid: serviceUuid
            }),
        ], function (err) {
            if (err) {
                console.log("setServices: Set services with error " + err);
            }
        });
    }
});
