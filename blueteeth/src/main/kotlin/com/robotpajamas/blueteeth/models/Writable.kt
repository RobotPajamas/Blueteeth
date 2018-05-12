package com.robotpajamas.blueteeth.models

import java.util.*

typealias WriteHandler = ((Result<Unit>) -> Unit)

interface Writable {
    enum class Type { WITH_RESPONSE, WITHOUT_RESPONSE }

    fun write(data: ByteArray,
              characteristic: UUID,
              service: UUID,
              type: Type = Type.WITHOUT_RESPONSE,
              block: WriteHandler? = null)
}