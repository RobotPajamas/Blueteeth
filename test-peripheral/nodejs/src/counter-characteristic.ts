import { Characteristic } from "bleno";
declare var Buffer: any;

export class CounterCharacteristic extends Characteristic {

    private data = Buffer.from([0]);

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
        console.log(`Read request issued - returning ${this.data.toString("hex")}`);
        callback(Characteristic.RESULT_SUCCESS, this.data);
    }

    public onWriteRequest(data: any, offset: any, withoutResponse: any, callback: any): void {
        if (data.length !== 1) {
            callback(Characteristic.RESULT_INVALID_ATTRIBUTE_LENGTH);
        }
        this.data[0] += data[0];
        console.log(`Write request issued with ${data.toString("hex")}`);
        callback(Characteristic.RESULT_SUCCESS);
    }
}
