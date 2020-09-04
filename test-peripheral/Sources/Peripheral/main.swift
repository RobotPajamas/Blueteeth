//
//  main.swift
//
//
//  Created by SJ on 2020-03-27.
//


import Foundation
import SwiftyTooth

SwiftyTooth.logger = MyLogger()
let instance = SwiftyTooth.shared

let serviceUuid = UUID(uuidString: "00726f62-6f74-7061-6a61-6d61732e6361")
let txUuid = UUID(uuidString: "01726f62-6f74-7061-6a61-6d61732e6361")
let rxUuid = UUID(uuidString: "02726f62-6f74-7061-6a61-6d61732e6361")

let rx = Characteristic(
    uuid: rxUuid!,
    properties: [
        .notify(onNotify: { (result) in
            print("In on notify \(result)")
        }),
        .read(onRead: { (response) in
            print("Returning counter value \(counter)")
            let data = Data([counter])
            response(.success(data))
        }),
    ]
)

let tx = Characteristic(
    uuid: txUuid!,
    properties: [
        .write(onWrite: { (request, response) in
            print("In on onWrite \(String(describing: request))")
            
            guard let value = request,
                value.count == 1 else {
                print("No data available")
                // TODO: Failure response?
                return
            }
            
            // TODO: Clean up this UInt8 
            increment(by: [UInt8](value)[0])
            
            // If increment is really long, this could technically time out - should increment the counter, respond success, THEN notify
            response(.success(()))
        }),

        .writeNoResponse(onWrite: { (request) in
            print("In on onWriteNoResponse \(String(describing: request))")
        })
    ]
)

var counter = UInt8.min
func increment(by value: UInt8) {
    for _ in 0..<127 { // Sending 128 elements to test notification queue
        counter += value
        let data = Data([counter])
        instance.emit(data: data, on: rx)
    }
    counter = UInt8.min
}

let service = Service(
    uuid: serviceUuid!,
    characteristics: [tx, rx]
)

instance.stateChangedHandler = { (state) in
    print("Current Bluetooth state is: \(state)")
}
instance.add(service: service)
instance.advertise(name: "SwiftyTooth")

RunLoop.main.run()
