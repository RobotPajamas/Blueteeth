package com.robotpajamas.blueteeth.models

import java.util.*

typealias WriteHandler = ((Result<Unit>) -> Unit)

enum class WriteType { WITH_RESPONSE, WITHOUT_RESPONSE }

interface Writable {
    fun write(data: ByteArray,
              characteristic: UUID,
              service: UUID,
              type: WriteType = WriteType.WITHOUT_RESPONSE,
              block: WriteHandler? = null)
}