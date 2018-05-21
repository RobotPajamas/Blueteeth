import { Characteristic } from "bleno";
declare var Buffer: any;

export class CounterCharacteristic extends Characteristic {

    private counter = Buffer.from([0]);
    private notify: any = undefined;

    constructor(uuid: string) {
        super({
            descriptors: [],
            properties: ["read", "write", "notify"],
            secure: [],
            uuid,
            value: null,
        });
    }

    public onReadRequest(offset: any, callback: any): void {
        console.log(`Read request issued - returning ${this.counter.toString("hex")}`);
        callback(Characteristic.RESULT_SUCCESS, this.counter);
    }

    public onWriteRequest(data: any, offset: any, withoutResponse: any, callback: any): void {
        if (data.length !== 1) {
            callback(Characteristic.RESULT_INVALID_ATTRIBUTE_LENGTH);
        }
        this.counter[0] += data[0];
        console.log(`Write request issued with ${data.toString("hex")}`);

        if (this.notify) {
            this.notify(this.counter);
        }
        callback(Characteristic.RESULT_SUCCESS);
    }

    public onSubscribe(maxValueSize: any, updateValueCallback: any): void {
        console.log("Client is subscribing to notifications");
        this.notify = updateValueCallback;
    }

    public onUnsubscribe() {
        console.log("Client is unsubscribing from notifications");
        this.notify = undefined;
    }

    public onIndicate(): void {
        console.log("Indicating client of updates");
    }

    public onNotify(): void {
        console.log("Notifying client of updates");
    }
}
