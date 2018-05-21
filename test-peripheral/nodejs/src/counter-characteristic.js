"use strict";
var __extends = (this && this.__extends) || (function () {
    var extendStatics = Object.setPrototypeOf ||
        ({ __proto__: [] } instanceof Array && function (d, b) { d.__proto__ = b; }) ||
        function (d, b) { for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p]; };
    return function (d, b) {
        extendStatics(d, b);
        function __() { this.constructor = d; }
        d.prototype = b === null ? Object.create(b) : (__.prototype = b.prototype, new __());
    };
})();
exports.__esModule = true;
var bleno_1 = require("bleno");
var CounterCharacteristic = /** @class */ (function (_super) {
    __extends(CounterCharacteristic, _super);
    function CounterCharacteristic(uuid) {
        var _this = _super.call(this, {
            descriptors: [],
            properties: ["read", "write", "notify"],
            secure: [],
            uuid: uuid,
            value: null
        }) || this;
        _this.counter = Buffer.from([0]);
        _this.notify = undefined;
        return _this;
    }
    CounterCharacteristic.prototype.onReadRequest = function (offset, callback) {
        console.log("Read request issued - returning " + this.counter.toString("hex"));
        callback(bleno_1.Characteristic.RESULT_SUCCESS, this.counter);
    };
    CounterCharacteristic.prototype.onWriteRequest = function (data, offset, withoutResponse, callback) {
        if (data.length !== 1) {
            callback(bleno_1.Characteristic.RESULT_INVALID_ATTRIBUTE_LENGTH);
        }
        this.counter[0] += data[0];
        console.log("Write request issued with " + data.toString("hex"));
        if (this.notify) {
            this.notify(this.counter);
        }
        callback(bleno_1.Characteristic.RESULT_SUCCESS);
    };
    CounterCharacteristic.prototype.onSubscribe = function (maxValueSize, updateValueCallback) {
        console.log("Client is subscribing to notifications");
        this.notify = updateValueCallback;
    };
    CounterCharacteristic.prototype.onUnsubscribe = function () {
        console.log("Client is unsubscribing from notifications");
        this.notify = undefined;
    };
    CounterCharacteristic.prototype.onIndicate = function () {
        console.log("Indicating client of updates");
    };
    CounterCharacteristic.prototype.onNotify = function () {
        console.log("Notifying client of updates");
    };
    return CounterCharacteristic;
}(bleno_1.Characteristic));
exports.CounterCharacteristic = CounterCharacteristic;
