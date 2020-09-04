//
//  logger.swift
//  
//
//  Created by SJ on 2020-03-27.
//

import Foundation
import SwiftyTooth

final class MyLogger: Logger {
    func verbose(_ message: String) {
        print("V: " + message)
    }
    func debug(_ message: String) {
        print("D: " + message)
    }
    func info(_ message: String) {
        print("I: " + message)
    }
    func warning(_ message: String) {
        print("W: " + message)
    }
    func error(_ message: String) {
        print("E: " + message)
    }
}
