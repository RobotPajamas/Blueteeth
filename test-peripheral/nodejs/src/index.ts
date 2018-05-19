import * as bleno from "bleno";
import { CounterCharacteristic } from "./counter-characteristic";

const peripheralName = "Blueteeth";
const serviceUuid = "00726f626f7470616a616d61732e6361";
const serviceUuids = [serviceUuid];

bleno.on("stateChange", (state: any) => {
    console.log(`Change state -> ${state}`);
    if (state === "poweredOn") {
        bleno.startAdvertising(peripheralName, serviceUuids, (error: any) => {
            if (error) {
                console.log(`startAdvertising: Advertising started with error: ${error}`);
            }
        });
    } else {
        bleno.stopAdvertising((error: any) => {
            if (error) {
                console.log(`stopAdvertising: Advertising stopped with error: ${error}`);
            }
        });
    }
});

bleno.on("advertisingStart", (error: any) => {
    if (error) {
        console.log(`onAdvertisingStart: Advertising error -> ${error}`);
    } else {
        console.log(`onAdvertisingStart: Advertising started -> name: ${peripheralName}`);
        console.log("onAdvertisingStart: Services running -> \n\t" + serviceUuids.join("\n\t"));
        bleno.setServices([
            new bleno.PrimaryService({
                characteristics: [
                    new CounterCharacteristic("01726f626f7470616a616d61732e6361"),
                ],
                uuid: serviceUuid,
            }),
        ], (err: any) => {
            if (err) {
                console.log(`setServices: Set services with error ${err}`);
            }
        });
    }
});
